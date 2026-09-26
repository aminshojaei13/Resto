package com.braveboy.calcuapp.data.remote

import com.braveboy.calcuapp.data.remote.dto.AccountingSummaryDto
import com.braveboy.calcuapp.data.remote.dto.AuthResponseDto
import com.braveboy.calcuapp.data.remote.dto.CheckoutRequestDto
import com.braveboy.calcuapp.data.remote.dto.CustomerDto
import com.braveboy.calcuapp.data.remote.dto.ExpenseDto
import com.braveboy.calcuapp.data.remote.dto.JournalEntryDto
import com.braveboy.calcuapp.data.remote.dto.LoginRequestDto
import com.braveboy.calcuapp.data.remote.dto.OrderDto
import com.braveboy.calcuapp.data.remote.dto.OrganizationDto
import com.braveboy.calcuapp.data.remote.dto.ProductDto
import com.braveboy.calcuapp.data.remote.dto.PurchaseDto
import com.braveboy.calcuapp.data.remote.dto.StockAdjustRequestDto
import com.braveboy.calcuapp.data.remote.dto.StoreDto
import com.braveboy.calcuapp.data.remote.dto.SupplierDto
import com.braveboy.calcuapp.data.remote.dto.WarehouseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CalcuappApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<AuthResponseDto>

    @GET("organizations")
    suspend fun getOrganizations(): Response<List<OrganizationDto>>

    @GET("organizations/{orgId}/stores")
    suspend fun getStores(@Path("orgId") orgId: String): Response<List<StoreDto>>

    @GET("stores/{storeId}/warehouses")
    suspend fun getWarehouses(@Path("storeId") storeId: String): Response<List<WarehouseDto>>

    @GET("products")
    suspend fun getProducts(
        @Query("org_id") orgId: String,
        @Query("query") query: String? = null,
        @Query("category") category: String? = null
    ): Response<List<ProductDto>>

    @GET("products/barcode/{barcode}")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String,
        @Query("org_id") orgId: String
    ): Response<ProductDto>

    @POST("products")
    suspend fun addProduct(@Body product: ProductDto): Response<ProductDto>

    @PUT("products/{id}")
    suspend fun updateProduct(@Path("id") id: String, @Body product: ProductDto): Response<ProductDto>

    @POST("inventory/adjust")
    suspend fun adjustStock(@Body request: StockAdjustRequestDto): Response<Map<String, Any>>

    @GET("orders")
    suspend fun getOrders(
        @Query("org_id") orgId: String,
        @Query("store_id") storeId: String? = null
    ): Response<List<OrderDto>>

    @POST("orders/checkout")
    suspend fun checkout(@Body request: CheckoutRequestDto): Response<OrderDto>

    @GET("customers")
    suspend fun getCustomers(
        @Query("org_id") orgId: String,
        @Query("query") query: String? = null
    ): Response<List<CustomerDto>>

    @POST("customers")
    suspend fun addCustomer(@Body customer: CustomerDto): Response<CustomerDto>

    @PUT("customers/{id}")
    suspend fun updateCustomer(@Path("id") id: String, @Body customer: CustomerDto): Response<CustomerDto>

    @GET("suppliers")
    suspend fun getSuppliers(@Query("org_id") orgId: String): Response<List<SupplierDto>>

    @POST("suppliers")
    suspend fun addSupplier(@Body supplier: SupplierDto): Response<SupplierDto>

    @PUT("suppliers/{id}")
    suspend fun updateSupplier(@Path("id") id: String, @Body supplier: SupplierDto): Response<SupplierDto>

    @DELETE("suppliers/{id}")
    suspend fun deleteSupplier(@Path("id") id: String): Response<Map<String, Any>>

    @GET("purchases")
    suspend fun getPurchases(@Query("org_id") orgId: String): Response<List<PurchaseDto>>

    @GET("expenses")
    suspend fun getExpenses(@Query("org_id") orgId: String): Response<List<ExpenseDto>>

    @POST("expenses")
    suspend fun addExpense(@Body expense: ExpenseDto): Response<ExpenseDto>

    @GET("accounting/journal")
    suspend fun getJournalEntries(@Query("org_id") orgId: String): Response<List<JournalEntryDto>>

    @GET("accounting/summary")
    suspend fun getAccountingSummary(@Query("org_id") orgId: String): Response<AccountingSummaryDto>
}
