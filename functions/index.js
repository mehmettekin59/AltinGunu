// Firebase Functions v6 syntax
const {onCall, HttpsError} = require('firebase-functions/v2/https');
const {onDocumentCreated, onDocumentWritten} = require('firebase-functions/v2/firestore');
const {onSchedule} = require('firebase-functions/v2/scheduler');
const {initializeApp} = require('firebase-admin/app');
const {getFirestore, FieldValue} = require('firebase-admin/firestore');
const {getMessaging} = require('firebase-admin/messaging');

// Admin SDK'yı başlat
initializeApp();

// Firestore ve Messaging referansları
const db = getFirestore();
const messaging = getMessaging();

// Yardımcı fonksiyonlar
function generateInviteCode() {
    const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    return Array.from({length: 8}, () => chars[Math.floor(Math.random() * chars.length)]).join('');
}

// 1. Davet oluşturma fonksiyonu
exports.createInvitation = onCall(
    {
        region: 'us-central1',
        cors: true,
    },
    async (request) => {
        console.log('createInvitation called with data:', request.data);

        const { drawGroupId, drawGroupName, inviterName } = request.data;

        if (!drawGroupId || !drawGroupName) {
            console.error('Missing required fields');
            throw new HttpsError('invalid-argument', 'DrawGroupId ve drawGroupName gerekli');
        }

        try {
            const inviteCode = generateInviteCode();
            const invitationId = db.collection('invitations').doc().id;

            const invitation = {
                id: invitationId,
                drawGroupId: drawGroupId,
                drawGroupName: drawGroupName,
                inviterName: inviterName || 'Grup Yöneticisi',
                inviteCode: inviteCode,
                expirationDate: Date.now() + (7 * 24 * 60 * 60 * 1000),
                createdDate: Date.now()
            };

            await db.collection('invitations').doc(invitationId).set(invitation);

            console.log('Invitation created successfully');

            return {
                success: true,
                inviteCode: inviteCode,
                invitation: invitation
            };

        } catch (error) {
            console.error('Error creating invitation:', error);
            throw new HttpsError('internal', `Davet oluşturulurken hata: ${error.message}`);
        }
    }
);

// 2. Katılım talebi gönderme
exports.submitParticipationRequest = onCall(
    {
        region: 'us-central1',
        cors: true,
    },
    async (request) => {
        const { inviteCode, participantName, fcmToken } = request.data;

        if (!inviteCode || !participantName || !fcmToken) {
            throw new HttpsError('invalid-argument', 'Tüm alanlar gerekli');
        }

        try {
            const inviteQuery = await db.collection('invitations')
                .where('inviteCode', '==', inviteCode)
                .where('expirationDate', '>', Date.now())
                .limit(1)
                .get();

            if (inviteQuery.empty) {
                throw new HttpsError('not-found', 'Geçersiz veya süresi dolmuş davet kodu');
            }

            const invitation = inviteQuery.docs[0].data();
            const requestId = db.collection('participation_requests').doc().id;

            const participationRequest = {
                id: requestId,
                drawGroupId: invitation.drawGroupId,
                participantName: participantName,
                fcmToken: fcmToken,
                inviteCode: inviteCode,
                status: 'PENDING',
                requestDate: Date.now()
            };

            await db.collection('participation_requests').doc(requestId).set(participationRequest);

            return {
                success: true,
                message: 'Katılım talebi gönderildi'
            };

        } catch (error) {
            console.error('Katılım talebi hatası:', error);
            if (error instanceof HttpsError) throw error;
            throw new HttpsError('internal', error.message);
        }
    }
);

// 3. Katılım taleplerini onaylama/reddetme
exports.approveParticipationRequest = onCall(
    {
        region: 'us-central1',
        cors: true,
    },
    async (request) => {
        const { requestId, approve } = request.data;

        if (!requestId || approve === undefined) {
            throw new HttpsError('invalid-argument', 'RequestId ve approve değeri gerekli');
        }

        try {
            const status = approve ? 'APPROVED' : 'REJECTED';

            await db.collection('participation_requests').doc(requestId).update({
                status: status,
                responseDate: Date.now()
            });

            return {
                success: true,
                message: approve ? 'Katılım talebi onaylandı' : 'Katılım talebi reddedildi'
            };

        } catch (error) {
            console.error('Katılım talebi işleme hatası:', error);
            throw new HttpsError('internal', error.message);
        }
    }
);

// 4. Grup bildirimi gönderme
exports.sendGroupNotification = onCall(
    {
        region: 'us-central1',
        cors: true,
    },
    async (request) => {
        const { groupId, title, message, extraData } = request.data;

        try {
            const groupDoc = await db.collection('draw_groups').doc(groupId).get();
            if (!groupDoc.exists) {
                throw new HttpsError('not-found', 'Grup bulunamadı');
            }

            const group = groupDoc.data();
            const participants = group.participants || [];

            if (participants.length === 0) {
                throw new HttpsError('failed-precondition', 'Grupta katılımcı yok');
            }

            const tokenPromises = participants.map(async (participant) => {
                const tokenQuery = await db.collection('fcm_tokens')
                    .where('participantId', '==', participant.id)
                    .where('isActive', '==', true)
                    .limit(1)
                    .get();

                if (!tokenQuery.empty) {
                    return tokenQuery.docs[0].data().token;
                }
                return null;
            });

            const tokens = (await Promise.all(tokenPromises)).filter(token => token !== null);

            if (tokens.length === 0) {
                throw new HttpsError('failed-precondition', 'Grupta aktif kullanıcı yok');
            }

            const notifications = tokens.map(token => ({
                token: token,
                title: title,
                message: message,
                data: {
                    groupId: groupId,
                    ...extraData
                },
                createdAt: FieldValue.serverTimestamp(),
                status: 'pending',
                retryCount: 0
            }));

            const batch = db.batch();
            notifications.forEach(notification => {
                const docRef = db.collection('notifications').doc();
                batch.set(docRef, notification);
            });

            await batch.commit();

            return {
                success: true,
                message: `${tokens.length} kullanıcıya bildirim gönderildi`
            };

        } catch (error) {
            console.error('Grup bildirimi hatası:', error);
            if (error instanceof HttpsError) throw error;
            throw new HttpsError('internal', error.message);
        }
    }
);

// 5. FCM Token güncelleme
exports.updateFcmToken = onCall(
    {
        region: 'us-central1',
        cors: true,
    },
    async (request) => {
        const { participantId, token } = request.data;

        if (!participantId || !token) {
            throw new HttpsError('invalid-argument', 'Katılımcı ID ve token gerekli');
        }

        try {
            const oldTokensQuery = await db.collection('fcm_tokens')
                .where('participantId', '==', participantId)
                .where('isActive', '==', true)
                .get();

            const batch = db.batch();

            oldTokensQuery.docs.forEach(doc => {
                batch.update(doc.ref, { isActive: false });
            });

            const tokenRef = db.collection('fcm_tokens').doc(token);
            batch.set(tokenRef, {
                token: token,
                participantId: participantId,
                isActive: true,
                updatedAt: FieldValue.serverTimestamp(),
                createdAt: FieldValue.serverTimestamp()
            }, { merge: true });

            await batch.commit();

            return { success: true, message: 'Token güncellendi' };

        } catch (error) {
            console.error('Token güncelleme hatası:', error);
            throw new HttpsError('internal', error.message);
        }
    }
);

// 6. Davet kodu doğrulama
exports.validateInviteCode = onCall(
    {
        region: 'us-central1',
        cors: true,
    },
    async (request) => {
        const { inviteCode } = request.data;

        if (!inviteCode) {
            throw new HttpsError('invalid-argument', 'Davet kodu gerekli');
        }

        try {
            const inviteQuery = await db.collection('invitations')
                .where('inviteCode', '==', inviteCode)
                .where('expirationDate', '>', Date.now())
                .limit(1)
                .get();

            if (inviteQuery.empty) {
                return {
                    valid: false,
                    message: 'Geçersiz veya süresi dolmuş davet kodu'
                };
            }

            const invitation = inviteQuery.docs[0].data();

            return {
                valid: true,
                invitation: invitation
            };

        } catch (error) {
            console.error('Davet kodu doğrulama hatası:', error);
            throw new HttpsError('internal', error.message);
        }
    }
);

// 7. Onaylanmış katılımcıları getirme
exports.getAcceptedParticipants = onCall(
    {
        region: 'us-central1',
        cors: true,
    },
    async (request) => {
        const { groupId } = request.data;

        if (!groupId) {
            throw new HttpsError('invalid-argument', 'Grup ID gerekli');
        }

        try {
            const participantsQuery = await db.collection('participation_requests')
                .where('drawGroupId', '==', groupId)
                .where('status', '==', 'ACCEPTED')
                .get();

            const participants = participantsQuery.docs.map(doc => ({
                id: doc.id,
                ...doc.data()
            }));

            return {
                success: true,
                participants: participants
            };

        } catch (error) {
            console.error('Katılımcıları getirme hatası:', error);
            throw new HttpsError('internal', error.message);
        }
    }
);

// 8. Mevcut davet kodunu getirme
exports.getExistingInviteCode = onCall(
    {
        region: 'us-central1',
        cors: true,
    },
    async (request) => {
        const { groupId } = request.data;

        if (!groupId) {
            throw new HttpsError('invalid-argument', 'Grup ID gerekli');
        }

        try {
            const inviteQuery = await db.collection('invitations')
                .where('drawGroupId', '==', groupId)
                .where('expirationDate', '>', Date.now())
                .orderBy('expirationDate', 'desc')
                .limit(1)
                .get();

            if (inviteQuery.empty) {
                return {
                    success: true,
                    inviteCode: null
                };
            }

            const invitation = inviteQuery.docs[0].data();

            return {
                success: true,
                inviteCode: invitation.inviteCode,
                expirationDate: invitation.expirationDate,
                remainingDays: Math.floor((invitation.expirationDate - Date.now()) / (1000 * 60 * 60 * 24))
            };

        } catch (error) {
            console.error('Davet kodu getirme hatası:', error);
            throw new HttpsError('internal', error.message);
        }
    }
);

// 9. Bildirim gönderme (Firestore trigger) - ÖNEMLİ FONKSİYON
exports.sendNotification = onDocumentCreated(
    {
        document: 'notifications/{notificationId}',
        region: 'us-central1',
    },
    async (event) => {
        const snapshot = event.data;
        if (!snapshot) {
            console.log('No data associated with the event');
            return;
        }

        const notification = snapshot.data();
        const notificationId = event.params.notificationId;

        console.log(`Yeni bildirim: ${notificationId}`);

        try {
            const message = {
                token: notification.token,
                notification: {
                    title: notification.title,
                    body: notification.message,
                },
                data: notification.data || {},
                android: {
                    priority: 'high',
                    notification: {
                        sound: 'default',
                        clickAction: 'FLUTTER_NOTIFICATION_CLICK',
                        channelId: 'gold_day_notifications'
                    }
                }
            };

            const response = await messaging.send(message);
            console.log(`Bildirim gönderildi: ${response}`);

            await snapshot.ref.update({
                status: 'sent',
                sentAt: FieldValue.serverTimestamp(),
                messageId: response
            });

        } catch (error) {
            console.error('Bildirim gönderme hatası:', error);
            await snapshot.ref.update({
                status: 'failed',
                failedAt: FieldValue.serverTimestamp(),
                error: error.message,
                retryCount: notification.retryCount || 0
            });
        }
    }
);

// 10. Başarısız bildirimleri yeniden deneme - ÖNEMLİ FONKSİYON
exports.retryFailedNotifications = onSchedule(
    {
        schedule: 'every 30 minutes',
        region: 'us-central1',
        timeZone: 'Europe/Istanbul',
    },
    async (event) => {
        console.log('Başarısız bildirimler kontrol ediliyor');

        const fifteenMinutesAgo = new Date();
        fifteenMinutesAgo.setMinutes(fifteenMinutesAgo.getMinutes() - 15);

        try {
            const failedQuery = await db.collection('notifications')
                .where('status', '==', 'failed')
                .where('failedAt', '<=', fifteenMinutesAgo)
                .where('retryCount', '<', 3)
                .limit(10)
                .get();

            console.log(`${failedQuery.size} başarısız bildirim yeniden denenecek`);

            const retryPromises = failedQuery.docs.map(async (doc) => {
                const notification = doc.data();

                const message = {
                    token: notification.token,
                    notification: {
                        title: notification.title,
                        body: notification.message,
                    },
                    data: notification.data || {},
                    android: {
                        priority: 'high',
                        notification: {
                            sound: 'default',
                            clickAction: 'FLUTTER_NOTIFICATION_CLICK',
                            channelId: 'gold_day_notifications'
                        }
                    }
                };

                try {
                    const response = await messaging.send(message);
                    console.log(`Yeniden deneme başarılı: ${response}`);

                    await doc.ref.update({
                        status: 'sent',
                        sentAt: FieldValue.serverTimestamp(),
                        messageId: response,
                        retriedAt: FieldValue.serverTimestamp()
                    });
                } catch (error) {
                    console.error(`Yeniden deneme başarısız: ${error.message}`);
                    await doc.ref.update({
                        retryCount: FieldValue.increment(1),
                        lastRetryAt: FieldValue.serverTimestamp(),
                        lastError: error.message
                    });
                }
            });

            await Promise.all(retryPromises);
            console.log('Başarısız bildirim kontrolü tamamlandı');

        } catch (error) {
            console.error('Başarısız bildirim kontrolü hatası:', error);
        }

        return null;
    }
);

// 11. Zamanlanmış hatırlatıcıları işleme
exports.processScheduledReminders = onDocumentCreated(
    {
        document: 'scheduled_reminders/{groupId}',
        region: 'us-central1',
    },
    async (event) => {
        const groupId = event.params.groupId;
        const snap = event.data;
        const reminderData = snap.data();
        const reminders = reminderData.reminders || [];

        console.log(`${groupId} grubu için ${reminders.length} hatırlatıcı zamanlanıyor`);

        try {
            const schedulePromises = reminders.map(async (reminder) => {
                const paymentDate = new Date(reminder.paymentDate);
                const reminderDate = new Date(paymentDate);
                reminderDate.setDate(reminderDate.getDate() - 1);

                if (reminderDate > new Date()) {
                    const scheduledNotification = {
                        groupId: groupId,
                        participantName: reminder.participantName,
                        amount: reminder.amount,
                        paymentDate: reminder.paymentDate,
                        itemType: reminder.itemType,
                        specificItem: reminder.specificItem,
                        scheduledFor: reminderDate,
                        status: 'scheduled',
                        createdAt: FieldValue.serverTimestamp()
                    };

                    return db.collection('scheduled_notifications')
                        .add(scheduledNotification);
                }
                return null;
            });

            await Promise.all(schedulePromises);

            await snap.ref.update({
                status: 'processed',
                processedAt: FieldValue.serverTimestamp()
            });

        } catch (error) {
            console.error('Hatırlatıcı işleme hatası:', error);
            await snap.ref.update({
                status: 'failed',
                error: error.message,
                failedAt: FieldValue.serverTimestamp()
            });
        }
    }
);

// 12. Günlük zamanlanmış hatırlatıcı kontrolü
exports.dailyReminderCheck = onSchedule(
    {
        schedule: '0 10 * * *',
        region: 'us-central1',
        timeZone: 'Europe/Istanbul',
    },
    async (event) => {
        console.log('Günlük hatırlatıcı kontrolü başladı');

        const today = new Date();
        const todayStart = new Date(today.getFullYear(), today.getMonth(), today.getDate());
        const todayEnd = new Date(todayStart);
        todayEnd.setDate(todayEnd.getDate() + 1);

        try {
            const scheduledQuery = await db.collection('scheduled_notifications')
                .where('scheduledFor', '>=', todayStart)
                .where('scheduledFor', '<', todayEnd)
                .where('status', '==', 'scheduled')
                .get();

            console.log(`${scheduledQuery.size} hatırlatıcı bugün gönderilecek`);

            const sendPromises = scheduledQuery.docs.map(async (doc) => {
                const reminder = doc.data();

                try {
                    // Grup bildirimini oluştur
                    const notificationData = {
                        groupId: reminder.groupId,
                        title: '🪙 Altın Günü Hatırlatması',
                        message: `${reminder.participantName} kişisi için ${reminder.amount} tutarında ödeme tarihi: ${reminder.paymentDate}`,
                        extraData: {
                            type: 'payment_reminder',
                            participant_name: reminder.participantName,
                            amount: reminder.amount.toString(),
                            payment_date: reminder.paymentDate,
                            item_type: reminder.itemType,
                            specific_item: reminder.specificItem || ''
                        }
                    };

                    // sendGroupNotification'ı çağır
                    const callableRef = await functions.httpsCallable('sendGroupNotification');
                    await callableRef(notificationData);

                    await doc.ref.update({
                        status: 'sent',
                        sentAt: FieldValue.serverTimestamp()
                    });

                    console.log(`Hatırlatıcı gönderildi: ${reminder.participantName}`);
                } catch (error) {
                    console.error(`Hatırlatıcı gönderme hatası: ${reminder.participantName}`, error);
                    await doc.ref.update({
                        status: 'failed',
                        error: error.message,
                        failedAt: FieldValue.serverTimestamp()
                    });
                }
            });

            await Promise.all(sendPromises);
            console.log('Günlük hatırlatıcı kontrolü tamamlandı');

        } catch (error) {
            console.error('Günlük hatırlatıcı kontrolü hatası:', error);
        }

        return null;
    }
);

// 13. Temizlik fonksiyonu
exports.cleanupOldData = onSchedule(
    {
        schedule: '0 3 * * *',
        region: 'us-central1',
        timeZone: 'Europe/Istanbul',
    },
    async (event) => {
        console.log('Eski veri temizliği başladı');

        const thirtyDaysAgo = new Date();
        thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

        const batch = db.batch();
        let deleteCount = 0;

        try {
            const oldNotifications = await db.collection('notifications')
                .where('status', '==', 'sent')
                .where('sentAt', '<', thirtyDaysAgo)
                .limit(100)
                .get();

            oldNotifications.docs.forEach(doc => {
                batch.delete(doc.ref);
                deleteCount++;
            });

            const expiredInvites = await db.collection('invitations')
                .where('expirationDate', '<', Date.now())
                .limit(50)
                .get();

            expiredInvites.docs.forEach(doc => {
                batch.delete(doc.ref);
                deleteCount++;
            });

            await batch.commit();
            console.log(`Temizlik tamamlandı. ${deleteCount} kayıt silindi.`);

        } catch (error) {
            console.error('Temizlik hatası:', error);
        }

        return null;
    }
);

// Test fonksiyonu
exports.simpleTest = onCall(
    {
        region: 'us-central1',
        cors: true,
    },
    async (request) => {
        console.log('simpleTest called');
        return {
            success: true,
            message: 'Test başarılı',
            timestamp: new Date().toISOString()
        };
    }
);