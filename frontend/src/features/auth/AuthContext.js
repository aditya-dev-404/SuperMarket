import { createContext } from 'react';

/** Holds { user, status, login, register, logout, endSession, setUser }. `register` creates an unverified account and does not log in. Use the useAuth hook to read it. */
export const AuthContext = createContext(null);