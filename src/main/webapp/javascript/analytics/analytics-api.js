/**
 * Analytics API Client
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Licensed under the Apache License, Version 2.0
 */

const AnalyticsAPI = (function() {
  'use strict';

  /**
   * Fetch data from an analytics endpoint
   */
  function fetchData(endpoint, params = {}) {
    return new Promise((resolve, reject) => {
      const queryString = Object.keys(params)
        .filter(key => params[key] !== '')
        .map(key => encodeURIComponent(key) + '=' + encodeURIComponent(params[key]))
        .join('&');

      const url = endpoint + (queryString ? '?' + queryString : '');

      fetch(url)
        .then(response => {
          if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
          }
          return response.json();
        })
        .then(data => {
          if (data.success === false) {
            reject(new Error(data.message || 'API error'));
          } else {
            resolve(data);
          }
        })
        .catch(error => {
          console.error('Fetch error:', error);
          reject(error);
        });
    });
  }

  /**
   * Get available filter options
   */
  function getFilterOptions() {
    return fetchData('/json/analyticsFiltersOptions');
  }

  // Public API
  return {
    fetchData: fetchData,
    getFilterOptions: getFilterOptions
  };
})();
