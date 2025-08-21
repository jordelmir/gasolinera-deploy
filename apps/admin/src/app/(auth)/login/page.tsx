// Ruta: apps/admin/src/app/(auth)/login/page.tsx
"use client";

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useAuthStore } from '@/store/authStore';
import apiClient from '@/lib/api/client';


export default function AuthenticationPage() {
  const router = useRouter();
  const loginAction = useAuthStore((state) => state.login);

  const [isLoading, setIsLoading] = useState(false);
  
  const [loginEmail, setLoginEmail] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  
  const [registerName, setRegisterName] = useState('');
  const [registerEmail, setRegisterEmail] = useState('');
  const [registerPassword, setRegisterPassword] = useState('');

  const handleLogin = async () => {
    setIsLoading(true);
    try {
      // Llamada real a la API a través de nuestro cliente centralizado
      const response = await apiClient.post('/auth/login', {
        email: loginEmail,
        password: loginPassword,
      });

      // El backend debería devolver algo como: { user: {...}, accessToken: "..." }
      const { user, accessToken } = response.data;

      // Guardar sesión en el store de Zustand
      loginAction(user, accessToken);

      // Redirigir al dashboard
      router.push('/dashboard');

    } catch (error) {
      console.error("Error en el inicio de sesión:", error);
      alert('Credenciales incorrectas o error en el servidor.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleRegister = async () => {
    setIsLoading(true);
    try {
        // Llamada real a la API para registrar
        await apiClient.post('/auth/register', {
            name: registerName,
            email: registerEmail,
            password: registerPassword
        });

        alert('¡Registro exitoso! Por favor, inicia sesión.');
        // Opcional: Iniciar sesión automáticamente después del registro
        
    } catch (error) {
        console.error("Error en el registro:", error);
        alert('No se pudo completar el registro. Inténtalo de nuevo.');
    } finally {
        setIsLoading(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-background">
      <Tabs defaultValue="login" className="w-[400px]">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="login">Iniciar Sesión</TabsTrigger>
          <TabsTrigger value="register">Registrarse</TabsTrigger>
        </TabsList>
        
        <TabsContent value="login">
          <Card>
            <CardHeader>
              <CardTitle>Bienvenido de Nuevo</CardTitle>
              <CardDescription>Ingresa tus credenciales para acceder a tu cuenta.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="login-email">Correo Electrónico</Label>
                <Input id="login-email" type="email" placeholder="tu@correo.com" value={loginEmail} onChange={(e) => setLoginEmail(e.target.value)} disabled={isLoading} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="login-password">Contraseña</Label>
                <Input id="login-password" type="password" value={loginPassword} onChange={(e) => setLoginPassword(e.target.value)} disabled={isLoading} />
              </div>
            </CardContent>
            <CardFooter className="flex flex-col gap-4">
              <Button className="w-full" onClick={handleLogin} disabled={isLoading}>
                {isLoading ? 'Ingresando...' : 'Iniciar Sesión'}
              </Button>
            </CardFooter>
          </Card>
        </TabsContent>

        <TabsContent value="register">
          <Card>
            <CardHeader>
              <CardTitle>Crear una Cuenta</CardTitle>
              <CardDescription>Es rápido y fácil. Empieza a acumular puntos hoy.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
                <div className="space-y-2">
                    <Label htmlFor="register-name">Nombre Completo</Label>
                    <Input id="register-name" placeholder="Tu Nombre" value={registerName} onChange={(e) => setRegisterName(e.target.value)} disabled={isLoading} />
                </div>
                <div className="space-y-2">
                    <Label htmlFor="register-email">Correo Electrónico</Label>
                    <Input id="register-email" type="email" placeholder="tu@correo.com" value={registerEmail} onChange={(e) => setRegisterEmail(e.target.value)} disabled={isLoading} />
                </div>
                <div className="space-y-2">
                    <Label htmlFor="register-password">Contraseña</Label>
                    <Input id="register-password" type="password" value={registerPassword} onChange={(e) => setRegisterPassword(e.target.value)} disabled={isLoading} />
                </div>
            </CardContent>
            <CardFooter>
              <Button className="w-full" onClick={handleRegister} disabled={isLoading}>
                {isLoading ? 'Creando...' : 'Crear Cuenta'}
              </Button>
            </CardFooter>
          </Card>
        </TabsContent>

      </Tabs>
    </div>
  );
}
