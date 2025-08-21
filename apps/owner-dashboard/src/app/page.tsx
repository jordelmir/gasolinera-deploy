'use client';

import React from 'react';
import {
  ChartBarIcon,
  BuildingStorefrontIcon,
  UsersIcon,
  TicketIcon,
  TrophyIcon,
  ArrowUpIcon,
  ArrowDownIcon,
} from '@heroicons/react/24/outline';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  BarChart,
  Bar,
} from 'recharts';

// Helper function to format numbers consistently
const formatNumber = (num: number): string => {
  return new Intl.NumberFormat('es-CR').format(num);
};

export default function DashboardPage() {
  // Mock data
  const mockData = {
    overview: {
      totalStations: 12,
      totalEmployees: 48,
      totalTicketsToday: 1247,
      totalRevenue: 2450000,
      weeklyGrowth: 12.5,
      monthlyGrowth: -3.2,
    },
    weeklyData: [
      { day: 'Lun', tickets: 180, revenue: 450000 },
      { day: 'Mar', tickets: 220, revenue: 550000 },
      { day: 'Mié', tickets: 190, revenue: 475000 },
      { day: 'Jue', tickets: 280, revenue: 700000 },
      { day: 'Vie', tickets: 320, revenue: 800000 },
      { day: 'Sáb', tickets: 380, revenue: 950000 },
      { day: 'Dom', tickets: 290, revenue: 725000 },
    ],
    topStations: [
      { name: 'Estación Centro', tickets: 245, revenue: 612500, growth: 15.2 },
      { name: 'Estación Norte', tickets: 198, revenue: 495000, growth: 8.7 },
      { name: 'Estación Sur', tickets: 167, revenue: 417500, growth: -2.1 },
      { name: 'Estación Este', tickets: 134, revenue: 335000, growth: 22.3 },
    ],
    employeePerformance: [
      { name: 'Juan Pérez', station: 'Centro', tickets: 45, conversion: 78 },
      { name: 'María González', station: 'Norte', tickets: 38, conversion: 82 },
      { name: 'Carlos Rodríguez', station: 'Sur', tickets: 42, conversion: 75 },
      { name: 'Ana Jiménez', station: 'Este', tickets: 35, conversion: 88 },
    ],
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-6">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">
                Dashboard Principal
              </h1>
              <p className="text-sm text-gray-500 mt-1">
                Resumen general de tu negocio
              </p>
            </div>
            <div className="flex items-center space-x-4">
              <button className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors">
                Generar Reporte
              </button>
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Overview Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-200">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Estaciones</p>
                <p className="text-2xl font-bold text-gray-900">
                  {mockData.overview.totalStations}
                </p>
              </div>
              <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
                <BuildingStorefrontIcon className="w-6 h-6 text-blue-600" />
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-200">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Empleados</p>
                <p className="text-2xl font-bold text-gray-900">
                  {mockData.overview.totalEmployees}
                </p>
              </div>
              <div className="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center">
                <UsersIcon className="w-6 h-6 text-green-600" />
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-200">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Tickets Hoy</p>
                <p className="text-2xl font-bold text-gray-900">
                  {formatNumber(mockData.overview.totalTicketsToday)}
                </p>
                <div className="flex items-center mt-1">
                  <ArrowUpIcon className="w-4 h-4 text-green-500" />
                  <span className="text-sm text-green-600 ml-1">
                    +{mockData.overview.weeklyGrowth}%
                  </span>
                </div>
              </div>
              <div className="w-12 h-12 bg-orange-100 rounded-lg flex items-center justify-center">
                <TicketIcon className="w-6 h-6 text-orange-600" />
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-200">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Ingresos</p>
                <p className="text-2xl font-bold text-gray-900">
                  ₡{formatNumber(mockData.overview.totalRevenue)}
                </p>
                <div className="flex items-center mt-1">
                  <ArrowDownIcon className="w-4 h-4 text-red-500" />
                  <span className="text-sm text-red-600 ml-1">
                    {mockData.overview.monthlyGrowth}%
                  </span>
                </div>
              </div>
              <div className="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center">
                <ChartBarIcon className="w-6 h-6 text-purple-600" />
              </div>
            </div>
          </div>
        </div>

        {/* Charts Section */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
          {/* Weekly Tickets Chart */}
          <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-200">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Tickets por Día (Esta Semana)
            </h3>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={mockData.weeklyData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="day" />
                <YAxis />
                <Tooltip />
                <Line
                  type="monotone"
                  dataKey="tickets"
                  stroke="#3B82F6"
                  strokeWidth={3}
                  dot={{ fill: '#3B82F6', strokeWidth: 2, r: 4 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>

          {/* Revenue Chart */}
          <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-200">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Ingresos por Día (Esta Semana)
            </h3>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={mockData.weeklyData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="day" />
                <YAxis />
                <Tooltip
                  formatter={(value) => [
                    `₡${Number(value).toLocaleString()}`,
                    'Ingresos',
                  ]}
                />
                <Bar dataKey="revenue" fill="#10B981" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Tables Section */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Top Stations */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200">
            <div className="p-6 border-b border-gray-200">
              <h3 className="text-lg font-semibold text-gray-900">
                Mejores Estaciones
              </h3>
            </div>
            <div className="p-6">
              <div className="space-y-4">
                {mockData.topStations.map((station, index) => (
                  <div
                    key={station.name}
                    className="flex items-center justify-between"
                  >
                    <div className="flex items-center">
                      <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center mr-3">
                        <span className="text-sm font-semibold text-blue-600">
                          {index + 1}
                        </span>
                      </div>
                      <div>
                        <p className="font-medium text-gray-900">
                          {station.name}
                        </p>
                        <p className="text-sm text-gray-500">
                          {station.tickets} tickets • ₡
                          {formatNumber(station.revenue)}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center">
                      {station.growth > 0 ? (
                        <ArrowUpIcon className="w-4 h-4 text-green-500" />
                      ) : (
                        <ArrowDownIcon className="w-4 h-4 text-red-500" />
                      )}
                      <span
                        className={`text-sm ml-1 ${
                          station.growth > 0 ? 'text-green-600' : 'text-red-600'
                        }`}
                      >
                        {station.growth > 0 ? '+' : ''}
                        {station.growth}%
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Employee Performance */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200">
            <div className="p-6 border-b border-gray-200">
              <h3 className="text-lg font-semibold text-gray-900">
                Rendimiento de Empleados
              </h3>
            </div>
            <div className="p-6">
              <div className="space-y-4">
                {mockData.employeePerformance.map((employee) => (
                  <div
                    key={employee.name}
                    className="flex items-center justify-between"
                  >
                    <div className="flex items-center">
                      <div className="w-10 h-10 bg-gray-200 rounded-full mr-3"></div>
                      <div>
                        <p className="font-medium text-gray-900">
                          {employee.name}
                        </p>
                        <p className="text-sm text-gray-500">
                          {employee.station}
                        </p>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className="font-medium text-gray-900">
                        {employee.tickets} tickets
                      </p>
                      <p className="text-sm text-gray-500">
                        {employee.conversion}% conversión
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Upcoming Raffles */}
        <div className="mt-8 bg-gradient-to-r from-blue-600 to-purple-600 rounded-xl shadow-sm p-6 text-white">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-xl font-bold mb-2">Próximos Sorteos</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="bg-white/10 rounded-lg p-4">
                  <div className="flex items-center mb-2">
                    <TrophyIcon className="w-5 h-5 mr-2" />
                    <span className="font-semibold">Sorteo Semanal</span>
                  </div>
                  <p className="text-2xl font-bold">₡40,000</p>
                  <p className="text-sm opacity-90">Domingo 15:00</p>
                </div>
                <div className="bg-white/10 rounded-lg p-4">
                  <div className="flex items-center mb-2">
                    <TrophyIcon className="w-5 h-5 mr-2" />
                    <span className="font-semibold">Sorteo Anual</span>
                  </div>
                  <p className="text-2xl font-bold">¡Un Carro!</p>
                  <p className="text-sm opacity-90">Diciembre 2024</p>
                </div>
              </div>
            </div>
            <div className="hidden md:block">
              <TrophyIcon className="w-24 h-24 opacity-20" />
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
