import SwiftUI

struct OrdersView: View {
    @State private var orders = [
        SalesOrder(id: "ord_1001", orderNumber: "ORD-2025-1001", customerName: "Sarah Connor", subtotal: 1549.98, taxAmount: 118.8, totalAmount: 1603.78, paymentMethod: "Card", paymentStatus: "PAID", fulfillmentStatus: "COMPLETED"),
        SalesOrder(id: "ord_1002", orderNumber: "ORD-2025-1002", customerName: "John Smith", subtotal: 999.0, taxAmount: 79.92, totalAmount: 1078.92, paymentMethod: "Mobile", paymentStatus: "PAID", fulfillmentStatus: "COMPLETED")
    ]
    
    var body: some View {
        List(orders) { order in
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(order.orderNumber)
                        .font(.headline)
                    Spacer()
                    Text(String(format: "$%.2f", order.totalAmount))
                        .font(.title3)
                        .bold()
                        .foregroundColor(.blue)
                }
                Text("Customer: \(order.customerName) • Via \(order.paymentMethod)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(.vertical, 4)
        }
    }
}
