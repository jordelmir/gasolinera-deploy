// Ruta: apps/admin-e2e/src/e2e/auth.spec.ts

import { test, expect } from '@playwright/test';


test.describe('Flujo de Autenticación', () => {
  
  test('un usuario debería poder iniciar sesión y ser redirigido al dashboard', async ({ page }) => {
    // 1. Navegar a la página de login
    await page.goto('/login');

    // 2. Verificar que estamos en la página correcta
    await expect(page.getByRole('heading', { name: 'Bienvenido de Nuevo' })).toBeVisible();

    // 3. Rellenar el formulario
    // (Usaremos credenciales de un usuario que debe existir gracias al script de seeding)
    await page.getByLabel('Correo Electrónico').fill('testuser@example.com');
    await page.getByLabel('Contraseña').fill('password123');

    // 4. Hacer clic en el botón de Iniciar Sesión
    await page.getByRole('button', { name: 'Iniciar Sesión' }).click();

    // 5. Verificar que fuimos redirigidos al dashboard
    await expect(page).toHaveURL('/dashboard');
    
    // 6. Verificar que el contenido del dashboard es visible
    await expect(page.getByRole('heading', { name: 'Dashboard Principal' })).toBeVisible();
  });

  // (Aquí podríamos añadir más tests, como "mostrar un error con credenciales incorrectas")

});
