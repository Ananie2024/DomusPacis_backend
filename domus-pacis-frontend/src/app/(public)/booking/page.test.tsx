import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import BookingPage from './page';
import { bookingApi } from '@/lib/api/bookingApi';
import { serviceAssetsApi } from '@/lib/api/serviceAssetsApi';

// Mock the APIs
jest.mock('@/lib/api/bookingApi');
jest.mock('@/lib/api/serviceAssetsApi');

// Mock next/navigation
const mockSearchParams = jest.fn();
jest.mock('next/navigation', () => ({
  useSearchParams: () => mockSearchParams(),
}));

describe('BookingPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (serviceAssetsApi.listAll as jest.Mock).mockResolvedValue({
      content: [
        { assetType: 'ROOM', isAvailable: true },
        { assetType: 'CONFERENCE_HALL', isAvailable: true },
      ],
    });
    (bookingApi.createBooking as jest.Mock).mockResolvedValue({});
  });

  it('selecting an asset type sets serviceAssetId correctly', async () => {
    render(<BookingPage />);

    // Wait for asset types to load
    await waitFor(() => {
      expect(screen.getByText('Room')).toBeInTheDocument();
    });

    // Select "Conference Hall"
    fireEvent.click(screen.getByText('Conference Hall'));

    // Navigate to step 2
    fireEvent.click(screen.getByText('Continue'));

    // Fill in dates
    await waitFor(() => {
      expect(screen.getByLabelText('Check-in Date')).toBeInTheDocument();
    });
    fireEvent.change(screen.getByLabelText('Check-in Date'), {
      target: { value: '2024-06-01' },
    });
    fireEvent.change(screen.getByLabelText('Check-out / End Date'), {
      target: { value: '2024-06-03' },
    });
    fireEvent.click(screen.getByText('Continue'));

    // Fill in guest details
    await waitFor(() => {
      expect(screen.getByPlaceholderText('John')).toBeInTheDocument();
    });
    fireEvent.change(screen.getByPlaceholderText('John'), {
      target: { value: 'Test' },
    });
    fireEvent.change(screen.getByPlaceholderText('Doe'), {
      target: { value: 'User' },
    });
    fireEvent.change(screen.getByPlaceholderText('john@example.com'), {
      target: { value: 'test@example.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('+250 78 000 0000'), {
      target: { value: '+250 78 000 0000' },
    });
    fireEvent.click(screen.getByText('Review'));

    // Submit the booking
    await waitFor(() => {
      expect(screen.getByText('Submit Booking')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByText('Submit Booking'));

    // Verify that the booking was submitted with the correct serviceAssetId
    await waitFor(() => {
      expect(bookingApi.createBooking).toHaveBeenCalledWith(
        expect.objectContaining({
          serviceAssetId: 'CONFERENCE_HALL',
        })
      );
    });
  });

  it('submitting without selecting an asset type is prevented', async () => {
    render(<BookingPage />);

    await waitFor(() => {
      expect(screen.getByText('Room')).toBeInTheDocument();
    });

    // The Continue button should be disabled when no asset is selected
    const continueButton = screen.getByText('Continue');
    expect(continueButton).toBeDisabled();
  });

  it('auto-advances to step 2 when asset and type query params are provided', async () => {
    // Mock search params to return asset and type
    mockSearchParams.mockReturnValue({
      get: (key: string) => {
        if (key === 'asset') return 'CONFERENCE_HALL';
        if (key === 'type') return 'CONFERENCE_HALL';
        return null;
      },
    });

    render(<BookingPage />);

    // Should directly show step 2 (Dates) without showing step 1
    await waitFor(() => {
      expect(screen.getByText('Select Dates')).toBeInTheDocument();
    });

    // Step 1 should NOT be visible
    expect(screen.queryByText('Choose a Service')).not.toBeInTheDocument();

    // Should be able to proceed directly to step 3
    fireEvent.click(screen.getByText('Continue'));
    
    await waitFor(() => {
      expect(screen.getByPlaceholderText('John')).toBeInTheDocument();
    });
  });
});
