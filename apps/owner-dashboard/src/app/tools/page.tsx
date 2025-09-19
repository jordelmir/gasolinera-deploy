import MessageImprover from '../components/MessageImprover'

export default function ToolsPage() {
  return (
    <div className="container mx-auto py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold">Herramientas</h1>
        <p className="text-gray-600 mt-2">
          Utiliza nuestras herramientas inteligentes para optimizar tu trabajo
        </p>
      </div>

      <MessageImprover />
    </div>
  )
}