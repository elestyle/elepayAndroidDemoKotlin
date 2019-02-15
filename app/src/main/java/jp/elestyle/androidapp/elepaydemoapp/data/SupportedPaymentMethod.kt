package jp.elestyle.androidapp.elepaydemoapp.data

import jp.elestyle.androidapp.elepaydemoapp.R

enum class SupportedPaymentMethod(val rawValue: String) {
    WECHAT_PAY("wechat"),
    ALIPAY("alipay"),
    CREDIT_CARD("creditcard"),
    UNION_PAY("unionpay"),
    PAYPAL("paypal"),
    LINE_PAY("linepay");

    companion object {
        fun from(string: String): SupportedPaymentMethod? = when (string.toLowerCase()) {
            "wechat pay", "wechatpay", "wx" -> WECHAT_PAY
            "alipay" -> ALIPAY
            "credit card", "creditcard" -> CREDIT_CARD
            "union pay", "unionpay" -> UNION_PAY
            "paypal" -> PAYPAL
            "linepay", "line" -> LINE_PAY
            else -> null
        }
    }

    fun associatedLocalisedResrouceId(): Int = when (this) {

        ALIPAY -> R.string.payment_method_alipay
        CREDIT_CARD -> R.string.payment_method_credit_card
        UNION_PAY -> R.string.payment_method_union_pay
        PAYPAL -> R.string.payment_method_paypal
        LINE_PAY -> R.string.payment_method_linepay
        else -> 0
    }
}