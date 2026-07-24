export interface DashboardSummary {
  incomeTotal: number;
  expenseTotal: number;
  balance: number;
  transactionCount: number;
  startDate: string;
  endDate: string;
}

export interface MonthlyBalance {
  year: number;
  month: number;
  incomeTotal: number;
  expenseTotal: number;
  balance: number;
}

export interface CategoryExpense {
  categoryId: number;
  categoryName: string;
  categoryColor: string;
  totalAmount: number;
  transactionCount: number;
}
