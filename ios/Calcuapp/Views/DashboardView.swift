import SwiftUI

struct DashboardView: View {
    @ObservedObject var viewModel: DashboardViewModel
    var isPersian: Bool = true
    
    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Metric Cards Row
                HStack(spacing: 12) {
                    MetricBox(title: isPersian ? "کل درآمد" : "Total Revenue", value: isPersian ? "۱۴۷,۰۴۱,۰۰۰ تومان" : String(format: "$%.2f", viewModel.totalRevenue), color: .blue)
                    MetricBox(title: isPersian ? "درآمد امروز" : "Today Sales", value: isPersian ? "۸۰,۱۸۹,۰۰۰ تومان" : String(format: "$%.2f", viewModel.todayRevenue), color: .cyan)
                }
                
                // Ledger Activity List
                VStack(alignment: .leading, spacing: 12) {
                    Text(isPersian ? "دفتر کل و اسناد مالی" : "General Ledger Activity")
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
                            Text(isPersian ? "\(Int(entry.amount * 50000).formatted()) تومان" : String(format: "$%.2f", entry.amount))
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
