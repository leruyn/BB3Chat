package com.bb3.bb3chat.core.platform

data class FakePushMessage(
    val title: String,
    val body: String
)

object FakePushTemplates {
    val all = listOf(
        FakePushMessage("Giao hàng thành công", "Đơn #VN28491 đã được giao. Cảm ơn bạn đã mua sắm!"),
        FakePushMessage("Ngân hàng", "Giao dịch chuyển khoản 500.000₫ thành công lúc 14:32."),
        FakePushMessage("Thời tiết", "Cảnh báo mưa lớn tại khu vực của bạn trong 2 giờ tới."),
        FakePushMessage("Tin tức", "Tin nóng: Chỉ số chứng khoán tăng mạnh phiên sáng."),
        FakePushMessage("Lịch họp", "Nhắc nhở: Họp team lúc 15:00 hôm nay.")
    )

    fun random(): FakePushMessage = all.random()
}

expect class FakePushNotifier {
    fun show(message: FakePushMessage)
}
