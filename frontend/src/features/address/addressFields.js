export const EMPTY_ADDRESS = {
  fullName: '',
  phone: '',
  line1: '',
  line2: '',
  city: '',
  state: '',
  pincode: '',
  defaultAddress: false,
};

const PHONE = /^[6-9]\d{9}$/;
const PINCODE = /^[1-9]\d{5}$/;

/** Mirrors the backend rules so the user gets feedback before the request; the API stays the authority. */
export function validateAddress(values) {
  const errors = {};
  if (!values.fullName.trim()) errors.fullName = 'Enter the recipient name';
  if (!PHONE.test(values.phone.trim())) errors.phone = 'Enter a valid 10-digit mobile number';
  if (!values.line1.trim()) errors.line1 = 'Enter the address';
  if (!values.city.trim()) errors.city = 'Enter the city';
  if (!values.state.trim()) errors.state = 'Enter the state';
  if (!PINCODE.test(values.pincode.trim())) errors.pincode = 'Enter a valid 6-digit PIN code';
  return errors;
}

/** Trims the text fields and drops an empty second line, as the API expects. */
export function toAddressRequest(values) {
  return {
    fullName: values.fullName.trim(),
    phone: values.phone.trim(),
    line1: values.line1.trim(),
    line2: values.line2.trim() || null,
    city: values.city.trim(),
    state: values.state.trim(),
    pincode: values.pincode.trim(),
    defaultAddress: values.defaultAddress,
  };
}

/** Form values from a saved address (null line2 becomes an empty input). */
export function toFormValues(address) {
  return { ...EMPTY_ADDRESS, ...address, line2: address.line2 ?? '' };
}