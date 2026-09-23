import SwiftUI

struct CustomerView: View {
    var isPersian: Bool = true
    
    @State private var customers = [
        Customer(id: "cust_1", organizationId: "org_apex", name: "سارا کنور", email: "sarah@example.com", phone: "۰۹۱۲۳۴۵۶۷۸۹", totalPurchases: 3548.5, loyaltyPoints: 350),
        Customer(id: "cust_2", organizationId: "org_apex", name: "رضا محمدی", email: "reza@example.ir", phone: "۰۹۹۸۷۶۵۴۳۲۱", totalPurchases: 1899.9, loyaltyPoints: 180)
    ]
    
    var body: some View {
        List(customers) { c in
            VStack(alignment: .leading, spacing: 6) {
                Text(c.name)
                    .font(.headline)
                if let phone = c.phone {
                    Text(phone)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                HStack {
                    Text(isPersian ? "مجموع خرید: \(Int(c.totalPurchases * 50000).formatted()) تومان" : "Total Spent: \(String(format: "$%.2f", c.totalPurchases))")
                        .font(.subheadline)
                        .bold()
                    Spacer()
                    Text("★ \(c.loyaltyPoints) \(isPersian ? "امتیاز" : "pts")")
                        .font(.caption)
                        .bold()
                        .foregroundColor(.blue)
                }
            }
            .padding(.vertical, 4)
        }
    }
}
