import { createContext } from 'react';

/** Holds { user, status, login, register, logout, setUser }. Use the useAuth hook to read it. */
export const AuthContext = createContext(null);
