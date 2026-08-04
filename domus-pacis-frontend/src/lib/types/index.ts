// ─── Enums ────────────────────────────────────────────────────────────────────

export enum UserRole {
  CUSTOMER      = 'CUSTOMER',
  STAFF         = 'STAFF',
  MANAGER       = 'MANAGER',
  FINANCE       = 'FINANCE',
  ADMIN         = 'ADMIN',
}

export enum BookingStatus {
  PENDING   = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  CHECKED_IN  = 'CHECKED_IN',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
}

export enum PaymentStatus {
  UNPAID  = 'UNPAID',
  PARTIAL = 'PARTIAL',
  PAID    = 'PAID',
  REFUNDED = 'REFUNDED',
}

export enum PaymentMethod {
  CASH          = 'CASH',
  BANK_TRANSFER = 'BANK_TRANSFER',
  MOBILE_MONEY  = 'MOBILE_MONEY',
  CARD          = 'CARD',
}

export enum AssetType {
  ROOM           = 'ROOM',
  CONFERENCE_HALL = 'CONFERENCE_HALL',
  WEDDING_GARDEN = 'WEDDING_GARDEN',
  RETREAT_CENTER = 'RETREAT_CENTER',
}

export enum StockMovementType {
  RECEIPT     = 'RECEIPT',
  CONSUMPTION = 'CONSUMPTION',
  ADJUSTMENT  = 'ADJUSTMENT',
  WASTE       = 'WASTE',
}

// ─── Common ───────────────────────────────────────────────────────────────────

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface ApiResponse<T> {
  data: T;
  message: string;
  success: boolean;
}

export interface ApiError {
  message: string;
  code: string;
  status: number;
  timestamp: string;
}

// ─── Auth ─────────────────────────────────────────────────────────────────────

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken:  string;
  refreshToken: string;
  userId:       string;
  email:        string;
  firstName:    string;
  lastName:     string;
  role:         string;
  expiresIn:    number;
}

export interface UserProfile {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  createdAt: string;
}

// ─── Service Assets ───────────────────────────────────────────────────────────

export interface ServiceAsset {
  id:           string;
  assetType:    AssetType;
  name:         string;
  description:  string;
  capacity:     number;
  pricePerUnit: number;
  pricingUnit:  string;
  isAvailable:  boolean;
}

// ─── Booking ──────────────────────────────────────────────────────────────────

export interface Booking {
  id: string;
  customerId: string;
  customerName: string;
  serviceAssetId: string;
  serviceAssetName: string;
  checkInDate: string;
  checkOutDate: string;
  numberOfGuests: number;
  status: BookingStatus;
  specialRequests?: string;
  totalAmount: number;
  createdAt: string;
}

export interface CreateBookingRequest {
  serviceAssetId: string;
  checkInDate: string;
  checkOutDate: string;
  numberOfGuests: number;
  specialRequests?: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
}

export interface AvailabilityRequest {
  serviceAssetId: string;
  checkIn: string;
  checkOut: string;
}

export interface AvailabilityResponse {
  assetId: string;
  checkIn: string;
  checkOut: string;
  available: boolean;
}

// ─── Address ──────────────────────────────────────────────────────────────────

export interface Address {
  street?:   string;
  city?:      string;
  province?:  string;
  country?:   string;
  postalCode?: string;
}

// ─── Customer ─────────────────────────────────────────────────────────────────

export interface Customer {
  id: string;
  userId?: string;
  fullName: string;
  email: string;
  phone: string;
  address?: Address;
  nationality?: string;
  idNumber?: string;
  segment?: string;
  totalBookings?: number;
  totalSpent?: number;
  createdAt: string;
}

export interface CreateCustomerRequest {
  fullName: string;
  email: string;
  phone: string;
  address?: Address;
  nationality?: string;
  idNumber?: string;
}

// ─── Staff ────────────────────────────────────────────────────────────────────

export interface Employee {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  role: EmployeeRole;
  department: string;
  hireDate: string;
  salary: number;
  isActive: boolean;
  schedules?: WorkSchedule[];
  createdAt: string;
}

export interface EmployeeRole {
  id: string;
  name: string;
  permissions: string[];
}

export interface WorkSchedule {
  id: string;
  employeeId: string;
  date: string;
  startTime: string;
  endTime: string;
  shiftType: string;
  notes?: string;
}

export interface PayrollRecord {
  id: string;
  employeeId: string;
  employee: Employee;
  period: string;
  basicSalary: number;
  allowances: number;
  deductions: number;
  netPay: number;
  status: 'PENDING' | 'PROCESSED' | 'PAID';
  processedAt?: string;
}

// ─── Inventory ────────────────────────────────────────────────────────────────

export interface InventoryItem {
  id: string;
  name: string;
  category: string;
  unit: string;
  currentStock: number;
  reorderLevel: number;
  unitCost: number;
  supplier?: Supplier;
  isLowStock: boolean;
  createdAt: string;
}

export interface StockMovement {
  id: string;
  inventoryItem: InventoryItem;
  movementType: StockMovementType;
  quantity: number;
  notes?: string;
  createdAt: string;
  createdBy: string;
}

export interface Supplier {
  id: string;
  name: string;
  contactPerson: string;
  phone: string;
  email: string;
  address: string;
}

// ─── Finance ──────────────────────────────────────────────────────────────────

export interface Payment {
  id: string;
  bookingId: string;
  amount: number;
  method: PaymentMethod;
  status: PaymentStatus;
  transactionRef?: string;
  paidAt?: string;
  createdAt: string;
}

export interface Invoice {
  id: string;
  invoiceNumber: string;
  bookingId: string;
  customer: Customer;
  items: InvoiceItem[];
  subtotal: number;
  taxAmount: number;
  totalAmount: number;
  issuedAt: string;
  dueDate: string;
}

export interface InvoiceItem {
  description: string;
  quantity: number;
  unitPrice: number;
  total: number;
}

export interface Expense {
  id: string;
  category: string;
  description: string;
  amount: number;
  date: string;
  approvedBy?: string;
  receiptUrl?: string;
  createdAt: string;
}

export interface RevenueTransaction {
  id: string;
  source: string;
  amount: number;
  date: string;
  bookingId?: string;
}

export interface FinancialSummary {
  totalRevenue: number;
  totalExpenses: number;
  netProfit: number;
  pendingPayments: number;
  revenueThisMonth: number;
  expensesThisMonth: number;
}

// ─── Tax ──────────────────────────────────────────────────────────────────────

export interface TaxRecord {
  id: string;
  invoiceId: string;
  taxType: string;
  rate: number;
  taxableAmount: number;
  taxAmount: number;
  period: string;
  status: 'PENDING' | 'FILED' | 'PAID';
}

export interface TaxSummary {
  period: string;
  vatCollected: number;
  withholdingTax: number;
  totalTaxLiability: number;
  filingDeadline: string;
  status: 'PENDING' | 'FILED' | 'OVERDUE';
}

// ─── Analytics ────────────────────────────────────────────────────────────────

export interface OccupancyData {
  date: string;
  rooms: number;
  conferences: number;
  gardens: number;
  retreats: number;
  overall: number;
}

export interface RevenueData {
  month: string;
  revenue: number;
  expenses: number;
  profit: number;
}

export interface ServicePopularity {
  name: string;
  bookings: number;
  revenue: number;
  percentage: number;
}

export interface DashboardKPIs {
  todayCheckIns: number;
  todayCheckOuts: number;
  pendingBookings: number;
  confirmedBookings: number;
  monthlyRevenue: number;
  monthlyExpenses: number;
  lowStockItems: number;
  totalCustomers: number;
  occupancyRate: number;
  revenueGrowth: number;
}
