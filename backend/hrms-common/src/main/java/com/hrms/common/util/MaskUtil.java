package com.hrms.common.util;

/**
 * Data masking utility for sensitive fields in API responses.
 *
 * <p>Rules:
 * <ul>
 *   <li>ID card: show first 3 + last 4, mask middle with ****</li>
 *   <li>Phone: show first 3 + last 4, mask middle with ****</li>
 *   <li>Account number: show last 4, mask rest with ****</li>
 * </ul>
 */
public final class MaskUtil {

    private MaskUtil() {
    }

    /**
     * Mask an ID card number.
     * Example: "110101199001011234" -> "110**********1234"
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 7) {
            return idCard;
        }
        int visiblePrefix = 3;
        int visibleSuffix = 4;
        int maskLen = idCard.length() - visiblePrefix - visibleSuffix;
        if (maskLen <= 0) {
            return idCard;
        }
        return idCard.substring(0, visiblePrefix)
                + "*".repeat(maskLen)
                + idCard.substring(idCard.length() - visibleSuffix);
    }

    /**
     * Mask a phone number.
     * Example: "13812345678" -> "138****5678"
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        int visiblePrefix = 3;
        int visibleSuffix = 4;
        int maskLen = phone.length() - visiblePrefix - visibleSuffix;
        if (maskLen <= 0) {
            return phone;
        }
        return phone.substring(0, visiblePrefix)
                + "*".repeat(maskLen)
                + phone.substring(phone.length() - visibleSuffix);
    }

    /**
     * Mask a bank account number.
     * Example: "6222021234567890123" -> "***************0123"
     */
    public static String maskAccountNo(String accountNo) {
        if (accountNo == null || accountNo.length() <= 4) {
            return accountNo;
        }
        int visibleSuffix = 4;
        int maskLen = accountNo.length() - visibleSuffix;
        return "*".repeat(maskLen) + accountNo.substring(accountNo.length() - visibleSuffix);
    }
}
