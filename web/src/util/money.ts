export const moneyRound = (amount: number): number => {
  return Math.round((amount + Number.EPSILON) * 100) / 100;
};

export const moneyAdd = (a: number, b: number): number => {
  return moneyRound(a + b);
};

export const moneySubtract = (a: number, b: number): number => {
  return moneyRound(a - b);
};

export const moneyMultiply = (a: number, b: number): number => {
  return moneyRound(a * b);
};

export const formatMoney = (amount: number, isFa = false): string => {
  const rounded = moneyRound(amount);
  return isFa ? `${rounded.toLocaleString('fa-IR')} تومان` : `$${rounded.toFixed(2)}`;
};
