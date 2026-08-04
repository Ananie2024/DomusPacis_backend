import { z } from 'zod';
import { AssetType } from '@/lib/types';

export const loginSchema = z.object({
  email: z.string().email('Please enter a valid email address.'),
  password: z.string().min(6, 'Password must be at least 6 characters.'),
});

export type LoginFormData = z.infer<typeof loginSchema>;

export const bookingSchema = z.object({
  serviceAssetId: z.string()
    .min(1, 'Please select a service.')
    .refine(
      (val) => /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(val) || Object.values(AssetType).includes(val as AssetType),
      {
        message: 'Invalid service selection. Please book from a specific room, hall, or garden page.',
      }
    ),
  checkIn: z.string().min(1, 'Check-in date is required.'),
  checkOut: z.string().min(1, 'Check-out date is required.'),
  numberOfGuests: z.number().min(1, 'At least 1 guest is required.'),
  notes: z.string().optional().nullable(),
  firstName: z.string().min(1, 'First name is required.'),
  lastName: z.string().min(1, 'Last name is required.'),
  email: z.string().email('Please enter a valid email address.'),
  phone: z.string().min(1, 'Phone number is required.'),
}).refine(
  (data) => data.checkOut >= data.checkIn,
  {
    message: 'Check-out must be on or after check-in.',
    path: ['checkOut'],
  }
);

export type BookingFormData = z.infer<typeof bookingSchema>;
