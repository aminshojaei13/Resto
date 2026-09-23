import SwiftUI

struct OrdersView: View {
    var isPersian: Bool = true
    
    @State private var orders = [
        SalesOrder(id: "ord_1001", orderNumber: "ORD-1403-1001", customerName: "سارا کنور", subtotal: 1549.98, taxAmount: 118.8, totalAmount: 1603.78, paymentMethod: "کارت‌خوان", paymentStatus: "پرداخت شده", fulfillmentStatus: "تکمیل شده"),
        SalesOrder(id: "ord_1002", orderNumber: "ORD-1403-1002", customerName: "رضا محمدی", subtotal: 999.0, taxAmount: 79.92, totalAmount: 1078.92, paymentMethod: "نقدی", paymentStatus: "پرداخت شده", fulfillmentStatus: "تکمیل شده")
    ]
    
    var body: some View {
        List(orders) { order in
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(order.orderNumber)
                        .font(.headline)
                    Spacer()
                    Text(isPersian ? "\(Int(order.totalAmount * 50000).formatted()) تومان" : String(format: "$%.2f", order.totalAmount))
                        .font(.title3)
                        .bold()
                        .foregroundColor(.blue)
                }
                Text(isPersian ? "مشتری: \(order.customerName) • روش پرداخت: \(order.paymentMethod)" : "Customer: \(order.customerName) • Via \(order.paymentMethod)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(.vertical, 4)
        }
    }
}
