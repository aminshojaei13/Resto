import SwiftUI

struct InventoryView: View {
    @ObservedObject var viewModel: InventoryViewModel
    var isPersian: Bool = true
    
    var body: some View {
        List(viewModel.products) { product in
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(product.name)
                        .font(.headline)
                    Spacer()
                    Text(isPersian ? "موجودی: ۲۲ عدد" : "Stock: 22 pcs")
                        .font(.caption)
                        .bold()
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.blue.opacity(0.15))
                        .cornerRadius(8)
                }
                
                HStack {
                    Text("SKU: \(product.sku)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Spacer()
                    Text(isPersian ? "\(Int(product.price * 50000).formatted()) تومان" : String(format: "$%.2f", product.price))
                        .font(.subheadline)
                        .bold()
                        .foregroundColor(.blue)
                }
            }
            .padding(.vertical, 4)
        }
        .task {
            await viewModel.loadProducts()
        }
    }
}
