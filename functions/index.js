const functions = require('firebase-functions');
const admin = require('firebase-admin');

// Firebase Admin SDK'yı başlat
admin.initializeApp();

// Firestore ve Messaging referansları
const db = admin.firestore();
const messaging = admin.messaging();

// Davet kodu generate etme fonksiyonu - SERVER TARAFINDA YAPILIYOR
function generateInviteCode() {
    const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    return Array.from({length: 8}, () => chars[Math.floor(Math.random() * chars.length)]).join('');
}

// Yeni bildirim dökümanı oluşturulduğunda tetiklenir
exports.sendNotification = functions.firestore
    .document('notifications/{notificationId}')
    .onCreate(async (snapshot, context) => {
        const notification = snapshot.data();
        console.log('Yeni bildirim:', notification);

        // Bildirim mesajını hazırla
        const message = {
            token: notification.token,
            notification: {
                title: notification.title,
                body: notification.message,
                icon: 'ic_notification'
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
            // Bildirimi gönder
            const response = await messaging.send(message);
            console.log('Bildirim başarıyla gönderildi:', response);

            // Firestore'da durumu güncelle
            await snapshot.ref.update({
                status: 'sent',
                sentAt: admin.firestore.FieldValue.serverTimestamp(),
                messageId: response
            });
        } catch (error) {
            console.error('Bildirim gönderilemedi:', error);

            // Hata durumunu kaydet ve retry sayacını artır
            await snapshot.ref.update({
                status: 'failed',
                error: error.message,
                errorCode: error.code,
                failedAt: admin.firestore.FieldValue.serverTimestamp(),
                retryCount: admin.firestore.FieldValue.increment(1)
            });
        }
    });

// ✅ YENİ: Davet oluşturma fonksiyonu - SERVER TARAFINDA
exports.createInvitation = functions.https.onCall(async (data, context) => {
    const { drawGroupId, drawGroupName, inviterName } = data;

    if (!drawGroupId || !drawGroupName) {
        throw new functions.https.HttpsError('invalid-argument', 'DrawGroupId ve drawGroupName gerekli');
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
            expirationDate: Date.now() + (7 * 24 * 60 * 60 * 1000), // 7 gün
            createdDate: Date.now()
        };

        await db.collection('invitations').doc(invitationId).set(invitation);

        return {
            success: true,
            inviteCode: inviteCode,
            invitation: invitation
        };

    } catch (error) {
        console.error('Davet oluşturma hatası:', error);
        throw new functions.https.HttpsError('internal', error.message);
    }
});

// ✅ YENİ: Katılım talebi gönderme - SERVER TARAFINDA
exports.submitParticipationRequest = functions.https.onCall(async (data, context) => {
    const { inviteCode, participantName, fcmToken } = data;

    if (!inviteCode || !participantName || !fcmToken) {
        throw new functions.https.HttpsError('invalid-argument', 'Tüm alanlar gerekli');
    }

    try {
        // Davet kodunu doğrula
        const inviteQuery = await db.collection('invitations')
            .where('inviteCode', '==', inviteCode)
            .where('expirationDate', '>', Date.now())
            .limit(1)
            .get();

        if (inviteQuery.empty) {
            throw new functions.https.HttpsError('not-found', 'Geçersiz veya süresi dolmuş davet kodu');
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
        throw new functions.https.HttpsError('internal', error.message);
    }
});

// ✅ YENİ: Katılım taleplerini onaylama/reddetme - SERVER TARAFINDA
exports.approveParticipationRequest = functions.https.onCall(async (data, context) => {
    const { requestId, approve } = data;

    if (!requestId || approve === undefined) {
        throw new functions.https.HttpsError('invalid-argument', 'RequestId ve approve değeri gerekli');
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
        throw new functions.https.HttpsError('internal', error.message);
    }
});

// Grup bildirimi gönderme fonksiyonu
exports.sendGroupNotification = functions.https.onCall(async (data, context) => {
    const { groupId, title, message, extraData } = data;

    try {
        // Grup bilgilerini al
        const groupDoc = await db.collection('draw_groups').doc(groupId).get();
        if (!groupDoc.exists) {
            throw new functions.https.HttpsError('not-found', 'Grup bulunamadı');
        }

        const group = groupDoc.data();
        const participants = group.participants || [];

        if (participants.length === 0) {
            throw new functions.https.HttpsError('failed-precondition', 'Grupta katılımcı yok');
        }

        // Katılımcıların FCM token'larını al
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
            throw new functions.https.HttpsError('failed-precondition', 'Grupta aktif kullanıcı yok');
        }

        // Her token için bildirim oluştur
        const notifications = tokens.map(token => ({
            token: token,
            title: title,
            message: message,
            data: {
                groupId: groupId,
                ...extraData
            },
            createdAt: admin.firestore.FieldValue.serverTimestamp()
        }));

        // Bildirimleri Firestore'a toplu ekle (bu sendNotification trigger'ını tetikleyecek)
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
        throw new functions.https.HttpsError('internal', error.message);
    }
});

// Zamanlanmış hatırlatıcıları işleme
exports.processScheduledReminders = functions.firestore
    .document('scheduled_reminders/{groupId}')
    .onCreate(async (snapshot, context) => {
        const groupId = context.params.groupId;
        const reminderData = snapshot.data();
        const reminders = reminderData.reminders || [];

        console.log(`${groupId} grubu için ${reminders.length} hatırlatıcı zamanlanıyor`);

        try {
            // Her hatırlatıcı için zamanlanmış görev oluştur
            const schedulePromises = reminders.map(async (reminder, index) => {
                const paymentDate = new Date(reminder.paymentDate);
                const reminderDate = new Date(paymentDate);
                reminderDate.setDate(reminderDate.getDate() - 1); // 1 gün önce hatırlat

                // Eğer hatırlatma tarihi gelecekte ise, zamanlanmış bildirim oluştur
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
                        createdAt: admin.firestore.FieldValue.serverTimestamp()
                    };

                    return db.collection('scheduled_notifications')
                        .add(scheduledNotification);
                }
                return null;
            });

            await Promise.all(schedulePromises);

            // Hatırlatıcı işlendiğini işaretle
            await snapshot.ref.update({
                status: 'processed',
                processedAt: admin.firestore.FieldValue.serverTimestamp()
            });

        } catch (error) {
            console.error('Hatırlatıcı işleme hatası:', error);
            await snapshot.ref.update({
                status: 'failed',
                error: error.message,
                failedAt: admin.firestore.FieldValue.serverTimestamp()
            });
        }
    });

// Günlük zamanlanmış hatırlatıcı kontrolü
exports.dailyReminderCheck = functions.pubsub
    .schedule('every day 10:00')
    .timeZone('Europe/Istanbul')
    .onRun(async (context) => {
        console.log('Günlük hatırlatıcı kontrolü başladı');

        const today = new Date();
        const todayStart = new Date(today.getFullYear(), today.getMonth(), today.getDate());
        const todayEnd = new Date(todayStart);
        todayEnd.setDate(todayEnd.getDate() + 1);

        try {
            // Bugün gönderilmesi gereken hatırlatıcıları bul
            const scheduledQuery = await db.collection('scheduled_notifications')
                .where('scheduledFor', '>=', todayStart)
                .where('scheduledFor', '<', todayEnd)
                .where('status', '==', 'scheduled')
                .get();

            console.log(`${scheduledQuery.size} hatırlatıcı bugün gönderilecek`);

            // Her hatırlatıcı için bildirim gönder
            const sendPromises = scheduledQuery.docs.map(async (doc) => {
                const reminder = doc.data();

                try {
                    // Grup bildirimini tetikle
                    await exports.sendGroupNotification.run({
                        groupId: reminder.groupId,
                        title: '🪙 Altın Günü Hatırlatması',
                        message: `${reminder.participantName} kişisi için ${reminder.amount} tutarında ödeme tarihi: ${reminder.paymentDate}`,
                        extraData: {
                            type: 'payment_reminder',
                            participant_name: reminder.participantName,
                            amount: reminder.amount,
                            payment_date: reminder.paymentDate,
                            item_type: reminder.itemType,
                            specific_item: reminder.specificItem
                        }
                    });

                    // Hatırlatıcıyı gönderildi olarak işaretle
                    await doc.ref.update({
                        status: 'sent',
                        sentAt: admin.firestore.FieldValue.serverTimestamp()
                    });

                    console.log(`Hatırlatıcı gönderildi: ${reminder.participantName}`);
                } catch (error) {
                    console.error(`Hatırlatıcı gönderme hatası: ${reminder.participantName}`, error);
                    await doc.ref.update({
                        status: 'failed',
                        error: error.message,
                        failedAt: admin.firestore.FieldValue.serverTimestamp()
                    });
                }
            });

            await Promise.all(sendPromises);
            console.log('Günlük hatırlatıcı kontrolü tamamlandı');

        } catch (error) {
            console.error('Günlük hatırlatıcı kontrolü hatası:', error);
        }

        return null;
    });

// Başarısız bildirimleri yeniden deneme
exports.retryFailedNotifications = functions.pubsub
    .schedule('every 30 minutes')
    .onRun(async (context) => {
        console.log('Başarısız bildirimler kontrol ediliyor');

        const fifteenMinutesAgo = new Date();
        fifteenMinutesAgo.setMinutes(fifteenMinutesAgo.getMinutes() - 15);

        try {
            // 15 dakika önce başarısız olan ve 3'ten az denenen bildirimleri bul
            const failedQuery = await db.collection('notifications')
                .where('status', '==', 'failed')
                .where('failedAt', '<=', fifteenMinutesAgo)
                .where('retryCount', '<', 3)
                .limit(10)
                .get();

            console.log(`${failedQuery.size} başarısız bildirim yeniden denenecek`);

            // Her başarısız bildirimi yeniden dene
            const retryPromises = failedQuery.docs.map(async (doc) => {
                const notification = doc.data();

                // Bildirim mesajını yeniden hazırla
                const message = {
                    token: notification.token,
                    notification: {
                        title: notification.title,
                        body: notification.message,
                        icon: 'ic_notification'
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
                        sentAt: admin.firestore.FieldValue.serverTimestamp(),
                        messageId: response,
                        retriedAt: admin.firestore.FieldValue.serverTimestamp()
                    });
                } catch (error) {
                    console.error(`Yeniden deneme başarısız: ${error.message}`);
                    await doc.ref.update({
                        retryCount: admin.firestore.FieldValue.increment(1),
                        lastRetryAt: admin.firestore.FieldValue.serverTimestamp(),
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
    });

// FCM Token güncelleme fonksiyonu
exports.updateFcmToken = functions.https.onCall(async (data, context) => {
    const { participantId, token } = data;

    if (!participantId || !token) {
        throw new functions.https.HttpsError('invalid-argument', 'Katılımcı ID ve token gerekli');
    }

    try {
        // Eski tokenları deaktif et
        const oldTokensQuery = await db.collection('fcm_tokens')
            .where('participantId', '==', participantId)
            .where('isActive', '==', true)
            .get();

        const batch = db.batch();

        oldTokensQuery.docs.forEach(doc => {
            batch.update(doc.ref, { isActive: false });
        });

        // Yeni token'ı ekle veya güncelle
        const tokenRef = db.collection('fcm_tokens').doc(token);
        batch.set(tokenRef, {
            token: token,
            participantId: participantId,
            isActive: true,
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            createdAt: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });

        await batch.commit();

        return { success: true, message: 'Token güncellendi' };

    } catch (error) {
        console.error('Token güncelleme hatası:', error);
        throw new functions.https.HttpsError('internal', error.message);
    }
});

// Davet kodu doğrulama fonksiyonu
exports.validateInviteCode = functions.https.onCall(async (data, context) => {
    const { inviteCode } = data;

    if (!inviteCode) {
        throw new functions.https.HttpsError('invalid-argument', 'Davet kodu gerekli');
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
        throw new functions.https.HttpsError('internal', error.message);
    }
});

// Temizlik fonksiyonu - Eski verileri temizle
exports.cleanupOldData = functions.pubsub
    .schedule('every day 03:00')
    .timeZone('Europe/Istanbul')
    .onRun(async (context) => {
        console.log('Eski veri temizliği başladı');

        const thirtyDaysAgo = new Date();
        thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

        const batch = db.batch();
        let deleteCount = 0;

        try {
            // Eski gönderilmiş bildirimleri sil
            const oldNotifications = await db.collection('notifications')
                .where('status', '==', 'sent')
                .where('sentAt', '<', thirtyDaysAgo)
                .limit(100)
                .get();

            oldNotifications.docs.forEach(doc => {
                batch.delete(doc.ref);
                deleteCount++;
            });

            // Süresi dolmuş davetleri sil
            const expiredInvites = await db.collection('invitations')
                .where('expirationDate', '<', Date.now())
                .limit(50)
                .get();

            expiredInvites.docs.forEach(doc => {
                batch.delete(doc.ref);
                deleteCount++;
            });

            // Eski işlenmiş hatırlatıcıları sil
            const oldReminders = await db.collection('scheduled_reminders')
                .where('status', '==', 'processed')
                .where('processedAt', '<', thirtyDaysAgo)
                .limit(50)
                .get();

            oldReminders.docs.forEach(doc => {
                batch.delete(doc.ref);
                deleteCount++;
            });

            await batch.commit();
            console.log(`Temizlik tamamlandı. ${deleteCount} kayıt silindi.`);

        } catch (error) {
            console.error('Temizlik hatası:', error);
        }

        return null;
    });