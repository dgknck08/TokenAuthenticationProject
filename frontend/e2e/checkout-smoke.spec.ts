import { test, expect } from "@playwright/test";

test("checkout smoke", async ({ page }) => {
  await page.route("**/api/cart", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        items: [{ productId: 1, productName: "Aurora Strat X", quantity: 1, unitPrice: 1200, totalPrice: 1200 }],
        totalItems: 1,
        totalAmount: 1200,
        cartType: "guest"
      })
    });
  });

  await page.goto("/en/checkout");
  await page.getByPlaceholder("First name").fill("John");
  await page.getByPlaceholder("Last name").fill("Doe");
  await page.getByPlaceholder("Email").fill("john@example.com");
  await page.getByPlaceholder("Phone").fill("5551234567");
  await page.getByPlaceholder("Address").fill("123 Main Street");
  await page.getByPlaceholder("City").fill("Istanbul");
  await page.getByPlaceholder("Postal code").fill("34000");

  await expect(page.getByRole("button", { name: "Complete order" })).toBeVisible();
});
