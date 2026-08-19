export interface ZoneRating {
  zoneId?: number;
  zoneName: string;
  prefixPattern?: string;
  rawPrefixPattern?: string;
  description?: string;
  priority: number;
  prefixInputMode?: "MANUAL" | "DROPDOWN";
  selectedPrefixIds?: number[];
  selectedCountryIds?: number[];
}

export interface ZoneRatingResponse {
  data: ZoneRating[];
  totalRecords: number;
}
