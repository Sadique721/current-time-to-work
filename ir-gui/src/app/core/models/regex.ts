export const Regex = {
  numeric: /^[0-9]*$/,
  characterlength100: /^(?!.{101})/,
  character200: /^(?!.{201})/,
  characterlength225: /^(?!.{226})/,
  characterlength255: /^(?!.{256})/,
  decimalNumber: /^[0-9]\d*(\.\d{1,2})?$/,
  alphaNUmeric: /^[A-Za-z0-9]+$/,
  IPCodeRegex: /^[A-Za-z0-9]*$/,
  email: /^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$/,
};
