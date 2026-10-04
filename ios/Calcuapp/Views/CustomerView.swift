import SwiftUI

struct CustomerView: View {
    var isPersian: Bool = true
    
    @State private var customers: [Customer] = []
    
    var body: some View {
        Group {
            if customers.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: "person.2.slash")
                        .font(.largeTitle)
                        .foregroundColor(.gray)
                    Text(isPersian ? "هیچ مشتری ثبت نشده است" : "No Customers Found")
                        .font(.headline)
                        .foregroundColor(.secondary)
                }
            } else {
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
        .task {
            if let loaded = try? await ApiService.shared.fetchCustomers() {
                self.customers = loaded
            }
        }
    }
}
