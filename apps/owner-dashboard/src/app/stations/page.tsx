'use client';

import React, { useState } from 'react';
import {
  BuildingStorefrontIcon,
  PlusIcon,
  PencilIcon,
  TrashIcon,
  MapPinIcon,
  XMarkIcon,
  CheckIcon,
  ExclamationTriangleIcon,
} from '@heroicons/react/24/outline';

interface Station {
  id: string;
  name: string;
  address: string;
  latitude: number;
  longitude: number;
  isActive: boolean;
  employeeCount: number;
  todayTickets: number;
  todayRevenue: number;
  conversionRate: number;
}

interface NewStationForm {
  name: string;
  address: string;
  latitude: string;
  longitude: string;
}

const mockStations: Station[] = [
  {
    id: '1',
    name: 'Gasolinera JSM Centro',
    address: 'Avenida Central, San José, Costa Rica',
    latitude: 9.9281,
    longitude: -84.0907,
    isActive: true,
    employeeCount: 8,
    todayTickets: 245,
    todayRevenue: 612500,
    conversionRate: 78.5,
  },
  {
    id: '2',
    name: 'Gasolinera JSM Norte',
    address: 'Barrio Escalante, San José, Costa Rica',
    latitude: 9.935,
    longitude: -84.085,
    isActive: true,
    employeeCount: 6,
    todayTickets: 198,
    todayRevenue: 495000,
    conversionRate: 82.3,
  },
  {
    id: '3',
    name: 'Gasolinera JSM Sur',
    address: 'Desamparados, San José, Costa Rica',
    latitude: 9.9,
    longitude: -84.07,
    isActive: true,
    employeeCount: 5,
    todayTickets: 167,
    todayRevenue: 417500,
    conversionRate: 75.2,
  },
];

export default function StationsPage() {
  const [stations, setStations] = useState<Station[]>(mockStations);
  const [showAddModal, setShowAddModal] = useState(false);
  const [editingStation, setEditingStation] = useState<Station | null>(null);
  const [newStation, setNewStation] = useState<NewStationForm>({
    name: '',
    address: '',
    latitude: '',
    longitude: '',
  });
  const [errors, setErrors] = useState<Partial<NewStationForm>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const validateForm = (form: NewStationForm): boolean => {
    const newErrors: Partial<NewStationForm> = {};

    if (!form.name.trim()) {
      newErrors.name = 'El nombre es requerido';
    }
    if (!form.address.trim()) {
      newErrors.address = 'La dirección es requerida';
    }
    if (!form.latitude || isNaN(parseFloat(form.latitude))) {
      newErrors.latitude = 'Latitud válida requerida';
    }
    if (!form.longitude || isNaN(parseFloat(form.longitude))) {
      newErrors.longitude = 'Longitud válida requerida';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleAddStation = async () => {
    if (!validateForm(newStation)) return;

    setIsSubmitting(true);

    // Simular llamada a API
    await new Promise((resolve) => setTimeout(resolve, 1000));

    const station: Station = {
      id: Date.now().toString(),
      name: newStation.name,
      address: newStation.address,
      latitude: parseFloat(newStation.latitude),
      longitude: parseFloat(newStation.longitude),
      isActive: true,
      employeeCount: 0,
      todayTickets: 0,
      todayRevenue: 0,
      conversionRate: 0,
    };

    setStations([...stations, station]);
    setNewStation({ name: '', address: '', latitude: '', longitude: '' });
    setErrors({});
    setShowAddModal(false);
    setIsSubmitting(false);

    // Mostrar notificación de éxito
    alert('¡Estación creada exitosamente!');
  };

  const handleEditStation = (station: Station) => {
    setEditingStation(station);
  };

  const handleUpdateStation = async () => {
    if (!editingStation) return;

    setIsSubmitting(true);

    // Simular llamada a API
    await new Promise((resolve) => setTimeout(resolve, 800));

    setStations(
      stations.map((s) => (s.id === editingStation.id ? editingStation : s))
    );
    setEditingStation(null);
    setIsSubmitting(false);

    alert('¡Estación actualizada exitosamente!');
  };

  const handleDeleteStation = async (id: string) => {
    if (
      !confirm(
        '¿Estás seguro de eliminar esta estación? Esta acción no se puede deshacer.'
      )
    ) {
      return;
    }

    // Simular llamada a API
    await new Promise((resolve) => setTimeout(resolve, 500));

    setStations(stations.filter((s) => s.id !== id));
    alert('Estación eliminada exitosamente');
  };

  const toggleStationStatus = async (id: string) => {
    // Simular llamada a API
    await new Promise((resolve) => setTimeout(resolve, 300));

    setStations(
      stations.map((s) => (s.id === id ? { ...s, isActive: !s.isActive } : s))
    );
  };

  const resetForm = () => {
    setNewStation({ name: '', address: '', latitude: '', longitude: '' });
    setErrors({});
    setShowAddModal(false);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-6">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">
                Gestión de Estaciones
              </h1>
              <p className="text-sm text-gray-500 mt-1">
                Administra tus gasolineras y su rendimiento
              </p>
            </div>
            <button
              onClick={() => setShowAddModal(true)}
              className="bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 transition-all duration-200 flex items-center shadow-lg hover:shadow-xl transform hover:-translate-y-0.5"
            >
              <PlusIcon className="w-5 h-5 mr-2" />
              Nueva Estación
            </button>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
          <div className="bg-white rounded-xl shadow-lg p-6 border border-gray-100 hover:shadow-xl transition-shadow duration-200">
            <div className="flex items-center">
              <div className="p-3 bg-blue-100 rounded-lg">
                <BuildingStorefrontIcon className="w-8 h-8 text-blue-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">
                  Total Estaciones
                </p>
                <p className="text-3xl font-bold text-gray-900">
                  {stations.length}
                </p>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-lg p-6 border border-gray-100 hover:shadow-xl transition-shadow duration-200">
            <div className="flex items-center">
              <div className="p-3 bg-green-100 rounded-lg">
                <CheckIcon className="w-8 h-8 text-green-600" />
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">
                  Estaciones Activas
                </p>
                <p className="text-3xl font-bold text-gray-900">
                  {stations.filter((s) => s.isActive).length}
                </p>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-lg p-6 border border-gray-100 hover:shadow-xl transition-shadow duration-200">
            <div className="flex items-center">
              <div className="p-3 bg-orange-100 rounded-lg">
                <span className="text-2xl">👥</span>
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">
                  Total Empleados
                </p>
                <p className="text-3xl font-bold text-gray-900">
                  {stations.reduce((sum, s) => sum + s.employeeCount, 0)}
                </p>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-lg p-6 border border-gray-100 hover:shadow-xl transition-shadow duration-200">
            <div className="flex items-center">
              <div className="p-3 bg-purple-100 rounded-lg">
                <span className="text-2xl">🎫</span>
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-600">Tickets Hoy</p>
                <p className="text-3xl font-bold text-gray-900">
                  {stations.reduce((sum, s) => sum + s.todayTickets, 0)}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Stations Table */}
        <div className="bg-white rounded-xl shadow-lg border border-gray-100">
          <div className="p-6 border-b border-gray-200">
            <h3 className="text-lg font-semibold text-gray-900">
              Lista de Estaciones ({stations.length})
            </h3>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Estación
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Empleados
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Tickets Hoy
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Ingresos Hoy
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Conversión
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Estado
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Acciones
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {stations.map((station) => (
                  <tr
                    key={station.id}
                    className="hover:bg-gray-50 transition-colors duration-150"
                  >
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div>
                        <div className="text-sm font-medium text-gray-900">
                          {station.name}
                        </div>
                        <div className="text-sm text-gray-500 flex items-center">
                          <MapPinIcon className="w-4 h-4 mr-1" />
                          {station.address}
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className="text-sm font-semibold text-gray-900 bg-gray-100 px-2 py-1 rounded-full">
                        {station.employeeCount}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className="text-sm font-semibold text-blue-600 bg-blue-50 px-2 py-1 rounded-full">
                        {station.todayTickets}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-green-600">
                      ₡{station.todayRevenue.toLocaleString()}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span
                        className={`text-sm font-semibold px-2 py-1 rounded-full ${
                          station.conversionRate >= 80
                            ? 'text-green-700 bg-green-100'
                            : station.conversionRate >= 70
                            ? 'text-yellow-700 bg-yellow-100'
                            : 'text-red-700 bg-red-100'
                        }`}
                      >
                        {station.conversionRate}%
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <button
                        onClick={() => toggleStationStatus(station.id)}
                        className={`inline-flex px-3 py-1 text-xs font-semibold rounded-full transition-colors duration-200 ${
                          station.isActive
                            ? 'bg-green-100 text-green-800 hover:bg-green-200'
                            : 'bg-red-100 text-red-800 hover:bg-red-200'
                        }`}
                      >
                        {station.isActive ? 'Activa' : 'Inactiva'}
                      </button>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      <div className="flex space-x-3">
                        <button
                          onClick={() => handleEditStation(station)}
                          className="text-blue-600 hover:text-blue-900 hover:bg-blue-50 p-2 rounded-lg transition-all duration-200"
                          title="Editar estación"
                        >
                          <PencilIcon className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleDeleteStation(station.id)}
                          className="text-red-600 hover:text-red-900 hover:bg-red-50 p-2 rounded-lg transition-all duration-200"
                          title="Eliminar estación"
                        >
                          <TrashIcon className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Add Station Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center p-6 border-b border-gray-200">
              <h3 className="text-xl font-semibold text-gray-900">
                Nueva Estación
              </h3>
              <button
                onClick={resetForm}
                className="text-gray-400 hover:text-gray-600 hover:bg-gray-100 p-2 rounded-lg transition-all duration-200"
              >
                <XMarkIcon className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6">
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Nombre de la Estación *
                </label>
                <input
                  type="text"
                  value={newStation.name}
                  onChange={(e) =>
                    setNewStation({ ...newStation, name: e.target.value })
                  }
                  className={`w-full border-2 rounded-xl px-4 py-3 text-gray-900 placeholder-gray-400 focus:outline-none transition-all duration-200 ${
                    errors.name
                      ? 'border-red-300 focus:border-red-500 focus:ring-2 focus:ring-red-200 bg-red-50'
                      : 'border-gray-300 focus:border-blue-500 focus:ring-2 focus:ring-blue-200 bg-white'
                  }`}
                  placeholder="Ej: Gasolinera JSM Centro"
                />
                {errors.name && (
                  <div className="flex items-center mt-2 text-red-600">
                    <ExclamationTriangleIcon className="w-4 h-4 mr-1" />
                    <span className="text-sm">{errors.name}</span>
                  </div>
                )}
              </div>

              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Dirección Completa *
                </label>
                <textarea
                  value={newStation.address}
                  onChange={(e) =>
                    setNewStation({ ...newStation, address: e.target.value })
                  }
                  className={`w-full border-2 rounded-xl px-4 py-3 text-gray-900 placeholder-gray-400 focus:outline-none transition-all duration-200 resize-none ${
                    errors.address
                      ? 'border-red-300 focus:border-red-500 focus:ring-2 focus:ring-red-200 bg-red-50'
                      : 'border-gray-300 focus:border-blue-500 focus:ring-2 focus:ring-blue-200 bg-white'
                  }`}
                  rows={3}
                  placeholder="Ej: Avenida Central, San José, Costa Rica"
                />
                {errors.address && (
                  <div className="flex items-center mt-2 text-red-600">
                    <ExclamationTriangleIcon className="w-4 h-4 mr-1" />
                    <span className="text-sm">{errors.address}</span>
                  </div>
                )}
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-2">
                    Latitud *
                  </label>
                  <input
                    type="number"
                    step="0.000001"
                    value={newStation.latitude}
                    onChange={(e) =>
                      setNewStation({ ...newStation, latitude: e.target.value })
                    }
                    className={`w-full border-2 rounded-xl px-4 py-3 text-gray-900 placeholder-gray-400 focus:outline-none transition-all duration-200 ${
                      errors.latitude
                        ? 'border-red-300 focus:border-red-500 focus:ring-2 focus:ring-red-200 bg-red-50'
                        : 'border-gray-300 focus:border-blue-500 focus:ring-2 focus:ring-blue-200 bg-white'
                    }`}
                    placeholder="9.9281"
                  />
                  {errors.latitude && (
                    <div className="flex items-center mt-1 text-red-600">
                      <ExclamationTriangleIcon className="w-3 h-3 mr-1" />
                      <span className="text-xs">{errors.latitude}</span>
                    </div>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-2">
                    Longitud *
                  </label>
                  <input
                    type="number"
                    step="0.000001"
                    value={newStation.longitude}
                    onChange={(e) =>
                      setNewStation({
                        ...newStation,
                        longitude: e.target.value,
                      })
                    }
                    className={`w-full border-2 rounded-xl px-4 py-3 text-gray-900 placeholder-gray-400 focus:outline-none transition-all duration-200 ${
                      errors.longitude
                        ? 'border-red-300 focus:border-red-500 focus:ring-2 focus:ring-red-200 bg-red-50'
                        : 'border-gray-300 focus:border-blue-500 focus:ring-2 focus:ring-blue-200 bg-white'
                    }`}
                    placeholder="-84.0907"
                  />
                  {errors.longitude && (
                    <div className="flex items-center mt-1 text-red-600">
                      <ExclamationTriangleIcon className="w-3 h-3 mr-1" />
                      <span className="text-xs">{errors.longitude}</span>
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="flex justify-end space-x-3 p-6 border-t border-gray-200 bg-gray-50 rounded-b-xl">
              <button
                onClick={resetForm}
                disabled={isSubmitting}
                className="px-6 py-3 text-gray-700 bg-white border-2 border-gray-300 rounded-lg hover:bg-gray-50 hover:border-gray-400 transition-all duration-200 font-medium disabled:opacity-50"
              >
                Cancelar
              </button>
              <button
                onClick={handleAddStation}
                disabled={isSubmitting}
                className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-all duration-200 font-medium shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 disabled:opacity-50 disabled:transform-none flex items-center"
              >
                {isSubmitting ? (
                  <>
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                    Creando...
                  </>
                ) : (
                  <>
                    <CheckIcon className="w-4 h-4 mr-2" />
                    Crear Estación
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Edit Station Modal */}
      {editingStation && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md">
            <div className="flex justify-between items-center p-6 border-b border-gray-200">
              <h3 className="text-xl font-semibold text-gray-900">
                Editar Estación
              </h3>
              <button
                onClick={() => setEditingStation(null)}
                className="text-gray-400 hover:text-gray-600 hover:bg-gray-100 p-2 rounded-lg transition-all duration-200"
              >
                <XMarkIcon className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6">
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Nombre de la Estación
                </label>
                <input
                  type="text"
                  value={editingStation.name}
                  onChange={(e) =>
                    setEditingStation({
                      ...editingStation,
                      name: e.target.value,
                    })
                  }
                  className="w-full border-2 border-gray-300 rounded-xl px-4 py-3 text-gray-900 focus:border-blue-500 focus:ring-2 focus:ring-blue-200 focus:outline-none transition-all duration-200 bg-white"
                />
              </div>

              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Dirección
                </label>
                <textarea
                  value={editingStation.address}
                  onChange={(e) =>
                    setEditingStation({
                      ...editingStation,
                      address: e.target.value,
                    })
                  }
                  className="w-full border-2 border-gray-300 rounded-xl px-4 py-3 text-gray-900 focus:border-blue-500 focus:ring-2 focus:ring-blue-200 focus:outline-none transition-all duration-200 bg-white resize-none"
                  rows={3}
                />
              </div>
            </div>

            <div className="flex justify-end space-x-3 p-6 border-t border-gray-200 bg-gray-50 rounded-b-xl">
              <button
                onClick={() => setEditingStation(null)}
                disabled={isSubmitting}
                className="px-6 py-3 text-gray-700 bg-white border-2 border-gray-300 rounded-lg hover:bg-gray-50 hover:border-gray-400 transition-all duration-200 font-medium disabled:opacity-50"
              >
                Cancelar
              </button>
              <button
                onClick={handleUpdateStation}
                disabled={isSubmitting}
                className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-all duration-200 font-medium shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 disabled:opacity-50 disabled:transform-none flex items-center"
              >
                {isSubmitting ? (
                  <>
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                    Actualizando...
                  </>
                ) : (
                  <>
                    <CheckIcon className="w-4 h-4 mr-2" />
                    Actualizar
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
