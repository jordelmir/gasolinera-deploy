'use client'

import React, { useState } from 'react'

interface MessageImprovementResponse {
  originalMessage: string
  improvedMessage: string
  improvements: string[]
  confidence: number
}

export default function MessageImprover() {
  const [message, setMessage] = useState('')
  const [context, setContext] = useState('')
  const [targetAudience, setTargetAudience] = useState('')
  const [tone, setTone] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [result, setResult] = useState<MessageImprovementResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  const handleImprove = async () => {
    if (!message.trim()) return

    setIsLoading(true)
    setError(null)
    setResult(null)

    try {
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'
      const response = await fetch(`${apiUrl}/ad-engine/api/v1/messages/improve`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          message,
          context: context || undefined,
          targetAudience: targetAudience || undefined,
          tone: tone || undefined,
        }),
      })

      if (!response.ok) {
        throw new Error('Error al mejorar el mensaje')
      }

      const data: MessageImprovementResponse = await response.json()
      setResult(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error desconocido')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="max-w-4xl mx-auto p-6 bg-white rounded-lg shadow-md">
      <div className="mb-6">
        <h2 className="text-2xl font-bold mb-2">✨ Mejorar Mensaje</h2>
        <p className="text-gray-600">
          Optimiza tus mensajes con IA para mayor claridad y efectividad
        </p>
      </div>

      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium mb-2">
            Mensaje original
          </label>
          <textarea
            placeholder="Escribe tu mensaje aquí..."
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            rows={4}
            className="w-full p-3 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium mb-2">
              Contexto (opcional)
            </label>
            <textarea
              placeholder="Contexto adicional..."
              value={context}
              onChange={(e) => setContext(e.target.value)}
              rows={2}
              className="w-full p-3 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-2">
              Audiencia objetivo (opcional)
            </label>
            <textarea
              placeholder="Ej: clientes, empleados..."
              value={targetAudience}
              onChange={(e) => setTargetAudience(e.target.value)}
              rows={2}
              className="w-full p-3 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-2">
              Tono deseado (opcional)
            </label>
            <textarea
              placeholder="Ej: profesional, amigable..."
              value={tone}
              onChange={(e) => setTone(e.target.value)}
              rows={2}
              className="w-full p-3 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>

        <button
          onClick={handleImprove}
          disabled={!message.trim() || isLoading}
          className="w-full bg-blue-600 text-white py-3 px-4 rounded-md hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed flex items-center justify-center"
        >
          {isLoading ? (
            <>
              <span className="animate-spin mr-2">⏳</span>
              Mejorando...
            </>
          ) : (
            <>
              <span className="mr-2">✨</span>
              Mejorar Mensaje
            </>
          )}
        </button>

        {error && (
          <div className="p-4 bg-red-50 border border-red-200 rounded-md">
            <p className="text-red-600">{error}</p>
          </div>
        )}

        {result && (
          <div className="space-y-4">
            <div>
              <h3 className="text-lg font-semibold mb-2">Mensaje Mejorado</h3>
              <div className="p-4 bg-green-50 border border-green-200 rounded-md">
                <p className="text-green-800">{result.improvedMessage}</p>
              </div>
            </div>

            <div>
              <h3 className="text-lg font-semibold mb-2">Mejoras Aplicadas</h3>
              <div className="flex flex-wrap gap-2">
                {result.improvements.map((improvement, index) => (
                  <span key={index} className="bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-sm">
                    {improvement}
                  </span>
                ))}
              </div>
            </div>

            <div>
              <h3 className="text-lg font-semibold mb-2">Confianza</h3>
              <div className="w-full bg-gray-200 rounded-full h-2">
                <div
                  className="bg-blue-600 h-2 rounded-full"
                  style={{ width: `${result.confidence * 100}%` }}
                ></div>
              </div>
              <p className="text-sm text-gray-600 mt-1">
                {Math.round(result.confidence * 100)}% de confianza
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}