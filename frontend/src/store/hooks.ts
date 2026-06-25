import { useDispatch, useSelector, type TypedUseSelectorHook } from "react-redux";
import type { AppDispatch, RootState } from "./store";

/**
 * Typed `useDispatch` that accepts `AppDispatch` including async thunks.
 * Always use this instead of the raw `useDispatch` so thunk action creators
 * are accepted without type-casting.
 */
export const useAppDispatch = () => useDispatch<AppDispatch>();

/**
 * Typed `useSelector` bound to `RootState`.
 * Always use this instead of the raw `useSelector` to avoid casting.
 */
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
