export interface Employee {
  id?: number;
  firstName: string;
  lastName: string;
  email: string;
  position: string;
  salary: number | null;
  hireDate: string;
  status: string;
  departmentName: string;
}

export interface EmployeeMetrics {
  totalEmployees: number;
  activeEmployees: number;
  departments: number;
  recentHires: number;
}

export interface AskResponse {
  answerable: boolean;
  answer: string;
  totalMatches: number | null;
  employees: Employee[];
  interpretedAs: string[];
}
