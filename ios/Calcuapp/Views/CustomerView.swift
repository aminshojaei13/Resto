import SwiftUI

struct CustomerView: View {
    @State private var customers = [
        Customer(id: "cust_1", organizationId: "org_apex", name: "Sarah Connor", email: "sarah@example.com", phone: "+1 555-234-5678", totalPurchases: 3548.5, loyaltyPoints: 350),
        Customer(id: "cust_2", organizationId: "org_apex", name: "John Smith", email: "john@techcorp.io", phone: "+1 555-987-6543", totalPurchases: 1899.9, loyaltyPoints: 180)
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
                    Text("Total Spent: \(String(format: "$%.2f", c.totalPurchases))")
                        .font(.subheadline)
                        .bold()
                    Spacer()
                    Text("★ \(c.loyaltyPoints) pts")
                        .font(.caption)
                        .bold()
                        .foregroundColor(.blue)
                }
            }
            .padding(.vertical, 4)
        }
    }
}
