ALTER TABLE audit_logs
    DROP CONSTRAINT IF EXISTS audit_logs_action_check;

ALTER TABLE audit_logs
    ADD CONSTRAINT audit_logs_action_check
        CHECK (action IN (
            'USER_REGISTRATION',
            'USER_LOGIN_SUCCESS',
            'USER_LOGIN_FAILURE',
            'USER_LOGOUT',
            'USER_ACCOUNT_LOCKED',
            'USER_ACCOUNT_UNLOCKED',
            'PASSWORD_CHANGED',
            'TOKEN_REFRESH',
            'TOKEN_BLACKLISTED',
            'PROFILE_UPDATED',
            'ADMIN_PRODUCT_CREATED',
            'ADMIN_PRODUCT_UPDATED',
            'ADMIN_PRODUCT_DELETED',
            'ADMIN_INVENTORY_UPDATED',
            'ORDER_CREATED',
            'CHECKOUT_QUOTED',
            'WISHLIST_ITEM_ADDED',
            'WISHLIST_ITEM_REMOVED',
            'ORDER_PAYMENT_INITIATED',
            'ORDER_PAYMENT_CALLBACK_RECEIVED',
            'ORDER_PAYMENT_WEBHOOK_RECEIVED',
            'ORDER_PAID',
            'ORDER_PACKED',
            'ORDER_SHIPPED',
            'ORDER_DELIVERED',
            'ORDER_CANCELLED',
            'ORDER_REFUNDED',
            'ORDER_RETURN_REQUESTED',
            'ORDER_RETURN_APPROVED',
            'ORDER_RETURN_REJECTED',
            'ORDER_PAYMENT_FAILED',
            'EMAIL_VERIFICATION',
            'SUSPICIOUS_ACTIVITY'
        ));
