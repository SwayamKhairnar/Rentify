import { useState, useEffect } from 'react';
import { Search, SlidersHorizontal, BookOpen, Laptop, Bike, Camera, Sofa, Shirt, Dumbbell, Music, Package } from 'lucide-react';
import { itemService } from '../services/item.service';
import ItemCard from '../components/ItemCard';
import LoadingSpinner from '../components/LoadingSpinner';
import './Home.css';

const CATEGORIES = [
  { value: '', label: 'All', icon: Package },
  { value: 'textbooks', label: 'Textbooks', icon: BookOpen },
  { value: 'electronics', label: 'Electronics', icon: Laptop },
  { value: 'bikes', label: 'Bikes', icon: Bike },
  { value: 'cameras', label: 'Cameras', icon: Camera },
  { value: 'furniture', label: 'Furniture', icon: Sofa },
  { value: 'clothing', label: 'Clothing', icon: Shirt },
  { value: 'sports', label: 'Sports', icon: Dumbbell },
  { value: 'instruments', label: 'Instruments', icon: Music },
];

/**
 * Home page — displays hero section and browsable item listing grid.
 * Supports search, category filtering, and sorting.
 */
export default function Home() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('');
  const [sort, setSort] = useState('');
  const [pagination, setPagination] = useState({});

  useEffect(() => {
    fetchItems();
  }, [category, sort]);

  async function fetchItems(page = 1) {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (search) params.set('search', search);
      if (category) params.set('category', category);
      if (sort) params.set('sort', sort);
      params.set('page', page);
      params.set('limit', 12);

      const res = await itemService.getItems(params.toString());
      setItems(res.data || []);
      setPagination(res.pagination || {});
    } catch (err) {
      console.error('Failed to fetch items:', err);
      setItems([]);
    } finally {
      setLoading(false);
    }
  }

  function handleSearch(e) {
    e.preventDefault();
    fetchItems();
  }

  return (
    <div className="page">
      {/* Hero Section */}
      <section className="hero" id="hero-section">
        <div className="container">
          <div className="hero-content">
            <span className="hero-badge">Campus Marketplace</span>
            <h1 className="hero-title">
              Rent anything <br /> 
              on <span className="hero-highlight">Campus</span>
            </h1>
            <p className="hero-subtitle">
              The sustainable way for students to share resources.
            </p>

            <form className="hero-search" onSubmit={handleSearch} id="search-form">
              <input
                type="text"
                placeholder="Find textbooks, bikes, electronics..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="hero-search-input"
                id="search-input"
              />
              <button type="submit" className="btn btn-primary" id="search-btn">
                <Search size={18} />
                Search
              </button>
            </form>
          </div>
        </div>
      </section>

      {/* Category Filter */}
      <section className="container" id="categories-section">
        <div className="categories-bar">
          {CATEGORIES.map((cat) => {
            const Icon = cat.icon;
            return (
              <button
                key={cat.value}
                className={`category-chip ${category === cat.value ? 'active' : ''}`}
                onClick={() => setCategory(cat.value)}
                id={`cat-${cat.value || 'all'}`}
              >
                <Icon size={16} />
                {cat.label}
              </button>
            );
          })}
        </div>

        {/* Sort & Results Header */}
        <div className="results-header">
          <h2 className="results-title">
            {category ? `${category.charAt(0).toUpperCase() + category.slice(1)}` : 'All Items'}
            {pagination.total !== undefined && (
              <span className="results-count"> ({pagination.total} found)</span>
            )}
          </h2>
          <div className="results-sort">
            <SlidersHorizontal size={16} />
            <select
              value={sort}
              onChange={(e) => setSort(e.target.value)}
              className="form-select"
              id="sort-select"
            >
              <option value="">Newest first</option>
              <option value="price_asc">Price: Low to High</option>
              <option value="price_desc">Price: High to Low</option>
              <option value="oldest">Oldest first</option>
            </select>
          </div>
        </div>

        {/* Items Grid */}
        {loading ? (
          <LoadingSpinner />
        ) : items.length === 0 ? (
          <div className="empty-state">
            <h3>No items found</h3>
            <p>Try adjusting your search or filters, or be the first to list something!</p>
          </div>
        ) : (
          <>
            <div className="items-grid" id="items-grid">
              {items.map((item) => (
                <ItemCard key={item._id} item={item} />
              ))}
            </div>

            {pagination.pages > 1 && (
              <div className="pagination">
                {Array.from({ length: pagination.pages }, (_, i) => (
                  <button
                    key={i + 1}
                    className={`pagination-btn ${pagination.page === i + 1 ? 'active' : ''}`}
                    onClick={() => fetchItems(i + 1)}
                  >
                    {i + 1}
                  </button>
                ))}
              </div>
            )}
          </>
        )}
      </section>
    </div>
  );
}
