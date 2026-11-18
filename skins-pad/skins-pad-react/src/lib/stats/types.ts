export interface Summary { gross: number|null; net: number|null; commission: number|null; operations: number; ticket: number|null; units: number; conversion: number|null; averageCommission: number|null; created: number }
export interface Group { key: string; label: string; count: number; amount: number|null }
export interface Rank { key: string; label: string; image: string|null; units: number; income: number|null; state: string }
export interface Sale { paymentId: number; productId: number; date: string; name: string; image: string|null; category: string; sellerId: string; method: string; net: number|null; gross: number|null; state: string }
export interface Stats {
  custody: Group[];
  timezone: string; from: string|null; to: string; compareFrom: string|null; compareTo: string|null;
  current: Summary; previous: Summary|null;
  timeline: {date:string; gross:number|null; operations:number}[];
  categories: Group[]; skins: Rank[]; sellers: Rank[]; methods: Group[]; funnel: Group[];
  publicationStates: Group[]; rejectionReasons: Group[]; wear: Group[]; grades: Group[]; stattrak: Group[];
  attributes: {floatMin:number|null; floatMax:number|null; floatAverage:number|null; priceMin:number|null; priceMax:number|null; priceMedian:number|null};
  users: {registered:number; sellers:number; buyers:number; sellerPercent:number};
  alerts: {kind:string; label:string; count:number; href:string}[];
  sales: Sale[]; categoryOptions: {id:number; name:string}[]; warnings:string[]; errors:Record<string,string>;
}
