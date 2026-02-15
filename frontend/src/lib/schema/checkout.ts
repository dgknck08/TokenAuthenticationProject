import { z } from "zod";

export const checkoutSchema = z
  .object({
    firstName: z.string().min(2),
    lastName: z.string().min(2),
    email: z.string().email(),
    phone: z.string().min(7),
    address: z.string().min(5),
    city: z.string().min(2),
    postalCode: z.string().min(3),
    shippingMethod: z.enum(["standard", "express"]),
    paymentMethod: z.enum(["card", "cod"]),
    cardNumber: z.string().optional(),
    cardHolder: z.string().optional(),
    expiry: z.string().optional(),
    cvc: z.string().optional()
  })
  .superRefine((value, ctx) => {
    if (value.paymentMethod === "card") {
      if (!value.cardNumber || value.cardNumber.replace(/\s/g, "").length < 15) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, message: "Card number is invalid", path: ["cardNumber"] });
      }
      if (!value.cardHolder) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, message: "Card holder is required", path: ["cardHolder"] });
      }
    }
  });

export type CheckoutSchema = z.infer<typeof checkoutSchema>;
