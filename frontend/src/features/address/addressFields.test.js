import { describe, expect, it } from 'vitest';
import { EMPTY_ADDRESS, toAddressRequest, toFormValues, validateAddress } from './addressFields';

const valid = {
  ...EMPTY_ADDRESS,
  fullName: 'Aditya Kumar',
  phone: '9876543210',
  line1: 'Flat 12, Main Road',
  city: 'Greater Noida',
  state: 'Uttar Pradesh',
  pincode: '201310',
};

describe('validateAddress', () => {
  it('accepts a complete address', () => {
    expect(validateAddress(valid)).toEqual({});
  });

  it('flags every required field when empty', () => {
    expect(Object.keys(validateAddress(EMPTY_ADDRESS)).sort()).toEqual(
      ['city', 'fullName', 'line1', 'phone', 'pincode', 'state'].sort(),
    );
  });

  it('rejects a mobile number not starting with 6-9 and a PIN starting with 0', () => {
    const errors = validateAddress({ ...valid, phone: '1234567890', pincode: '012345' });
    expect(errors.phone).toBeDefined();
    expect(errors.pincode).toBeDefined();
  });
});

describe('toAddressRequest', () => {
  it('trims text and sends a blank second line as null', () => {
    const request = toAddressRequest({ ...valid, fullName: '  Aditya Kumar ', line2: '   ' });
    expect(request.fullName).toBe('Aditya Kumar');
    expect(request.line2).toBeNull();
  });
});

describe('toFormValues', () => {
  it('turns a null second line into an empty input', () => {
    expect(toFormValues({ ...valid, id: 4, line2: null }).line2).toBe('');
  });
});