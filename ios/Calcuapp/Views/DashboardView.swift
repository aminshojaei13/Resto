import SwiftUI

struct DashboardView: View {
    @ObservedObject var viewModel: DashboardViewModel
    var isPersian: Bool = true
    
    // Calcuapp Brand Color Tokens
    private let tealBrand = Color(red: 0.07, green: 0.69, blue: 0.75) // #12AFC0
    private let bgLight = Color(red: 0.96, green: 0.96, blue: 0.97)   // #F4F5F7
    
    var body: some View {
        ZStack {
            bgLight.ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: 20) {
                    // Header Title
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(isPersian ? "داشبورد مدیریتی و هوش مالی" : "Executive Business Dashboard")
                                .font(.title2)
                                .bold()
                                .foregroundColor(Color(red: 0.12, green: 0.16, blue: 0.22))
                            
                            Text(isPersian ? "خلاصه وضعیت درآمد، سود و تعهدات مالی" : "Revenue, gross margins, and operational summary")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                    }
                    .padding(.horizontal)
                    
                    // 4 KPI Cards Grid
                    VStack(spacing: 12) {
                        HStack(spacing: 12) {
                            SaaSMetricCard(
                                title: isPersian ? "کل فروش" : "Sales Revenue",
                                value: isPersian ? "۱۴۷,۰۴۱,۰۰۰ تومان" : String(format: "$%.2f", viewModel.totalRevenue),
                                badge: isPersian ? "فروش" : "Sales",
                                icon: "creditcard.fill",
                                color: tealBrand
                            )
                            
                            SaaSMetricCard(
                                title: isPersian ? "فروش امروز" : "Today Revenue",
                                value: isPersian ? "۸۰,۱۸۹,۰۰۰ تومان" : String(format: "$%.2f", viewModel.todayRevenue),
                                badge: isPersian ? "امروز" : "Today",
                                icon: "cart.fill",
                                color: .blue
                            )
                        }
                        
                        HStack(spacing: 12) {
                            SaaSMetricCard(
                                title: isPersian ? "سود خالص" : "Net Operating Profit",
                                value: isPersian ? "۴۲,۵۰۰,۰۰۰ تومان" : "$42,500.00",
                                badge: isPersian ? "سود" : "Profit",
                                icon: "chart.line.uptrend.xyaxis",
                                color: .green
                            )
                            
                            SaaSMetricCard(
                                title: isPersian ? "ارزش موجودی" : "Inventory Value",
                                value: isPersian ? "۲۲۵,۰۰۰,۰۰۰ تومان" : "$225,000.00",
                                badge: isPersian ? "انبار" : "Stock",
                                icon: "shippingbox.fill",
                                color: .orange
                            )
                        }
                    }
                    .padding(.horizontal)
                    
                    // Ledger Stream
                    VStack(alignment: .leading, spacing: 14) {
                        HStack {
                            Text(isPersian ? "آخرین اسناد دفتر کل و حسابداری" : "General Ledger Activity Log")
                                .font(.headline)
                                .bold()
                            Spacer()
                        }
                        .padding(.horizontal)
                        
                        VStack(spacing: 10) {
                            ForEach(viewModel.ledgerEntries) { entry in
                                HStack {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(entry.entryNumber)
                                            .font(.caption)
                                            .bold()
                                            .foregroundColor(tealBrand)
                                        Text(entry.description)
                                            .font(.subheadline)
                                            .fontWeight(.medium)
                                    }
                                    Spacer()
                                    Text(isPersian ? "\(Int(entry.amount * 50000).formatted()) تومان" : String(format: "$%.2f", entry.amount))
                                        .font(.headline)
                                        .bold()
                                        .foregroundColor(entry.type == "CREDIT" ? .green : .red)
                                }
                                .padding(16)
                                .background(Color.white)
                                .cornerRadius(12)
                                .shadow(color: Color.black.opacity(0.04), radius: 4, x: 0, y: 2)
                            }
                        }
                        .padding(.horizontal)
                    }
                }
                .padding(.vertical)
            }
        }
        .task {
            await viewModel.loadData()
        }
    }
}

struct SaaSMetricCard: View {
    let title: String
    let value: String
    let badge: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(color)
                    .font(.subheadline)
                Text(title)
                    .font(.caption)
                    .bold()
                    .foregroundColor(.secondary)
                Spacer()
            }
            
            Text(value)
                .font(.system(size: 16, weight: .bold, design: .rounded))
                .lineLimit(1)
                .minimumScaleFactor(0.8)
                .foregroundColor(Color(red: 0.12, green: 0.16, blue: 0.22))
        }
        .padding(16)
        .background(Color.white)
        .cornerRadius(16)
        .shadow(color: Color.black.opacity(0.04), radius: 4, x: 0, y: 2)
    }
}
