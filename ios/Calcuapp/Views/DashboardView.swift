import SwiftUI

struct DashboardView: View {
    @ObservedObject var viewModel: DashboardViewModel
    
    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Metric Cards Row
                HStack(spacing: 12) {
                    MetricBox(title: "Total Revenue", value: String(format: "$%.2f", viewModel.totalRevenue), color: .blue)
                    MetricBox(title: "Today Sales", value: String(format: "$%.2f", viewModel.todayRevenue), color: .cyan)
                }
                
                // Ledger Activity List
                VStack(alignment: .leading, spacing: 12) {
                    Text("General Ledger Activity")
                        .font(.headline)
                        .padding(.horizontal)
                    
                    ForEach(viewModel.ledgerEntries) { entry in
                        HStack {
                            VStack(alignment: .leading) {
                                Text(entry.entryNumber)
                                    .font(.caption)
                                    .bold()
                                Text(entry.description)
                                    .font(.subheadline)
                            }
                            Spacer()
                            Text(String(format: "$%.2f", entry.amount))
                                .font(.headline)
                                .foregroundColor(entry.type == "CREDIT" ? .green : .red)
                        }
                        .padding()
                        .background(Color(.secondarySystemBackground))
                        .cornerRadius(10)
                        .padding(.horizontal)
                    }
                }
            }
            .padding(.vertical)
        }
        .task {
            await viewModel.loadData()
        }
    }
}

struct MetricBox: View {
    let title: String
    let value: String
    let color: Color
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.caption)
                .bold()
            Text(value)
                .font(.title2)
                .bold()
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(color.opacity(0.15))
        .cornerRadius(12)
    }
}
