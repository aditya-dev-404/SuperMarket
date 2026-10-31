/** The lines of an address, shared by the address list, checkout and order pages. */
export default function AddressSummary({ address }) {
  return (
    <address className="text-sm not-italic text-gray-600">
      <span className="block font-medium text-gray-900">{address.fullName}</span>
      <span className="block">{address.line1}</span>
      {address.line2 && <span className="block">{address.line2}</span>}
      <span className="block">
        {address.city}, {address.state} {address.pincode}
      </span>
      <span className="block">Phone: {address.phone}</span>
    </address>
  );
}