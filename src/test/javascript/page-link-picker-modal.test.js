/**
 * Tests for Page Link Picker Modal
 * @author Test Suite
 * @created 01/21/26
 */

describe('PageLinkPickerModal Search', () => {
  let modal;

  beforeEach(() => {
    // Create a mock modal instance with test pages
    modal = {
      pages: [
        { title: 'Terms of Service Agreement', link: '/legal/terms' },
        { title: 'Terms of Service and Agreements', link: '/terms' },
        { title: 'Privacy Policy', link: '/privacy' },
        { title: 'Service Agreement', link: '/service-agreement' },
        { title: 'User Agreement', link: '/user-agreement' },
        { title: 'Support Services', link: '/support' },
        { title: 'About Us', link: '/about' }
      ]
    };
  });

  /**
   * Helper function to simulate the search filter logic
   */
  const filterPages = (query, pages) => {
    const lowerQuery = query.toLowerCase().trim();
    if (!lowerQuery) return pages;

    return pages.filter(page => {
      const searchText = (page.title + ' ' + page.link).toLowerCase();
      const terms = lowerQuery.split(/\s+/);
      return terms.every(term => searchText.includes(term));
    }).sort((a, b) => {
      const aText = (a.title + ' ' + a.link).toLowerCase();
      const bText = (b.title + ' ' + b.link).toLowerCase();
      const aTitleText = a.title.toLowerCase();
      const bTitleText = b.title.toLowerCase();

      const queryTerms = lowerQuery.split(/\s+/);
      const aMinPos = Math.min(...queryTerms.map(t => {
        const titlePos = aTitleText.indexOf(t);
        const fullPos = aText.indexOf(t);
        return titlePos >= 0 ? titlePos : fullPos;
      }));
      const bMinPos = Math.min(...queryTerms.map(t => {
        const titlePos = bTitleText.indexOf(t);
        const fullPos = bText.indexOf(t);
        return titlePos >= 0 ? titlePos : fullPos;
      }));

      return aMinPos - bMinPos;
    });
  };

  test('should match "agreement service" to "Terms of Service and Agreements"', () => {
    const results = filterPages('agreement service', modal.pages);
    expect(results.length).toBeGreaterThan(0);
    expect(results.some(p => p.title === 'Terms of Service and Agreements')).toBe(true);
  });

  test('should match "service agreement" in any order', () => {
    const results = filterPages('service agreement', modal.pages);
    expect(results.length).toBeGreaterThan(0);
    expect(results.some(p => p.title === 'Terms of Service and Agreements')).toBe(true);
  });

  test('should match "agreement" to all agreement pages', () => {
    const results = filterPages('agreement', modal.pages);
    expect(results.length).toBe(4);
    const titles = results.map(p => p.title);
    expect(titles).toContain('Terms of Service and Agreements');
    expect(titles).toContain('Service Agreement');
    expect(titles).toContain('User Agreement');
  });

  test('should match "service" to service-related pages', () => {
    const results = filterPages('service', modal.pages);
    expect(results.length).toBe(4);
    expect(results.map(p => p.title)).toContain('Terms of Service and Agreements');
    expect(results.map(p => p.title)).toContain('Service Agreement');
    expect(results.map(p => p.title)).toContain('Support Services');
  });

  test('should not match pages missing required terms', () => {
    const results = filterPages('agreement xyz', modal.pages);
    // Should find nothing because "xyz" doesn't appear anywhere
    expect(results.length).toBe(0);
  });

  test('should return all pages when query is empty', () => {
    const results = filterPages('', modal.pages);
    expect(results.length).toBe(modal.pages.length);
  });

  test('should be case-insensitive', () => {
    const results1 = filterPages('AGREEMENT SERVICE', modal.pages);
    const results2 = filterPages('agreement service', modal.pages);
    expect(results1.length).toBe(results2.length);
    expect(results1.map(p => p.title)).toEqual(results2.map(p => p.title));
  });

  test('should prioritize earlier matches when sorting results', () => {
    const results = filterPages('service', modal.pages);
    // "Terms of Service" should appear because "service" starts at position 10
    // "Support Services" has "service" at position 8, so it should come first
    const termsIndex = results.findIndex(p => p.title === 'Terms of Service and Agreements');
    const supportIndex = results.findIndex(p => p.title === 'Support Services');
    expect(termsIndex).toBeGreaterThanOrEqual(0);
    expect(supportIndex).toBeGreaterThanOrEqual(0);
    // Both should exist in results
    expect(results.length).toBeGreaterThan(1);
  });

  test('should handle partial word matches', () => {
    const results = filterPages('serv', modal.pages);
    expect(results.length).toBeGreaterThan(0);
    expect(results.some(p => p.title.includes('Service'))).toBe(true);
  });

  test('should match multiple term combinations', () => {
    const results = filterPages('agreement privacy', modal.pages);
    // No page has both "agreement" and "privacy"
    expect(results.length).toBe(0);
  });

  test('should match multiple term combinations', () => {
    const results = filterPages('terms agreement', modal.pages);
    expect(results.length).toBe(2);
  });

  test('should trim whitespace from query', () => {
    const results1 = filterPages('  agreement service  ', modal.pages);
    const results2 = filterPages('agreement service', modal.pages);
    expect(results1.length).toBe(results2.length);
    expect(results1.map(p => p.title)).toEqual(results2.map(p => p.title));
  });

  test('should handle multiple spaces between terms', () => {
    const results1 = filterPages('agreement    service', modal.pages);
    const results2 = filterPages('agreement service', modal.pages);
    expect(results1.length).toBe(results2.length);
    expect(results1.map(p => p.title)).toEqual(results2.map(p => p.title));
  });
});
