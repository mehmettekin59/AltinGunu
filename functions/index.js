const functions = require('firebase-functions');
const admin = require('firebase-admin');

// Firebase Admin SDK'yı başlat
admin.initializeApp();

// Firestore ve Messaging referansları
const db = admin.firestore();
const messaging = admin.messaging();

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
                icon: 'ic_notification' // Android'de gösterilecek ikon
            },
            data: notification.data || {},
            android: {
                priority: 'high',
                notification: {
                    sound: 'default',
                    clickAction: 'FLUTTER_NOTIFICATION_CLICK'
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

            // Hata durumunu kaydet
            await snapshot.ref.update({
                status: 'failed',
                error: error.message,
                errorCode: error.code,
                failedAt: admin.firestore.FieldValue.serverTimestamp()
            });
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
        const tokens = group.fcmTokens || [];

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

        // Bildirimleri Firestore'a toplu ekle
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

// Zamanlanmış hatırlatıcılar için (opsiyonel)
exports.scheduledReminders = functions.pubsub
    .schedule('every day 10:00')
    .timeZone('Europe/Istanbul')
    .onRun(async (context) => {
        console.log('Günlük hatırlatıcı kontrolü başladı');

        const today = new Date();
        const tomorrow = new Date(today);
        tomorrow.setDate(tomorrow.getDate() + 1);

        // Yarın ödemesi olan grupları bul
        const groupsSnapshot = await db.collection('draw_groups')
            .where('isActive', '==', true)
            .where('nextPaymentDate', '>=', today)
            .where('nextPaymentDate', '<=', tomorrow)
            .get();

        const notifications = [];

        groupsSnapshot.forEach(doc => {
            const group = doc.data();
            const tokens = group.fcmTokens || [];

            tokens.forEach(token => {
                notifications.push({
                    token: token,
                    title: '🪙 Altın Günü Hatırlatması',
                    message: `Yarın ${group.nextPaymentPerson} için ödeme günü!`,
                    data: {
                        groupId: doc.id,
                        type: 'reminder'
                    },
                    createdAt: admin.firestore.FieldValue.serverTimestamp()
                });
            });
        });

        // Bildirimleri toplu ekle
        if (notifications.length > 0) {
            const batch = db.batch();
            notifications.forEach(notification => {
                const docRef = db.collection('notifications').doc();
                batch.set(docRef, notification);
            });
            await batch.commit();
            console.log(`${notifications.length} hatırlatıcı oluşturuldu`);
        }

        return null;
    });