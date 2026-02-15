"use client";

import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { SORT_OPTIONS } from "@/lib/constants";
import { ProductFilters } from "@/types/api";

interface ShopFiltersProps {
  filters: ProductFilters;
  brands: string[];
  categories: string[];
  onChange: (next: ProductFilters) => void;
}

export function ShopFilters({ filters, brands, categories, onChange }: ShopFiltersProps) {
  return (
    <Card className="space-y-4 p-4">
      <div>
        <p className="text-xs uppercase tracking-[0.18em] text-primary">Filtreler</p>
        <p className="mt-1 text-sm text-foreground/70">Model, marka, kategori ve fiyata göre daraltın.</p>
      </div>

      <Input
        placeholder="Modele göre ara"
        value={filters.query || ""}
        onChange={(e) => onChange({ ...filters, query: e.target.value, page: 1 })}
      />

      <Select
        value={filters.brand || ""}
        onChange={(e) => onChange({ ...filters, brand: e.target.value || undefined, page: 1 })}
      >
        <option value="">Tüm markalar</option>
        {brands.map((brand) => (
          <option key={brand} value={brand}>
            {brand}
          </option>
        ))}
      </Select>

      <Select
        value={filters.category || ""}
        onChange={(e) => onChange({ ...filters, category: e.target.value || undefined, page: 1 })}
      >
        <option value="">Tüm kategoriler</option>
        {categories.map((category) => (
          <option key={category} value={category}>
            {category}
          </option>
        ))}
      </Select>

      <div className="grid grid-cols-2 gap-2">
        <Input
          type="number"
          placeholder="En az"
          value={filters.minPrice ?? ""}
          onChange={(e) => onChange({ ...filters, minPrice: Number(e.target.value) || undefined, page: 1 })}
        />
        <Input
          type="number"
          placeholder="Maks"
          value={filters.maxPrice ?? ""}
          onChange={(e) => onChange({ ...filters, maxPrice: Number(e.target.value) || undefined, page: 1 })}
        />
      </div>

      <Select
        value={filters.sort || "featured"}
        onChange={(e) => onChange({ ...filters, sort: e.target.value as ProductFilters["sort"] })}
      >
        {SORT_OPTIONS.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </Select>

      <Button variant="outline" className="w-full" onClick={() => onChange({ page: 1, limit: 9, sort: "featured" })}>
        Filtreleri sıfırla
      </Button>
    </Card>
  );
}
