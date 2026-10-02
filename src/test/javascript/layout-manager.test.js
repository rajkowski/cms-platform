/**
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Licensed under the Apache License, Version 2.0
 * 
 * Info Tab Manager
 * Manages the Info tab content and page metadata editing in the visual editor
 * 
 * @author matt rajkowski
 * @created 1/10/26 12:00 PM
 */

const { readFileSync } = require('fs');
const { join } = require('path');

const source = readFileSync(join(__dirname, '../../main/webapp/javascript/widgets/editor/layout-manager.js'), 'utf8');
const LayoutManager = new Function(`${source}\nreturn LayoutManager;`)();

describe('LayoutManager role attributes', () => {
  const createManager = () => new LayoutManager(null, { get: () => ({}) });

  test.each([
    ['populated', ' role="Editor,Admin"', ['Editor', 'Admin']],
    ['empty', ' role=""', []],
    ['absent', '', []]
  ])('round-trips %s roles through XML and publish JSON', (name, attribute, expectedRoles) => {
    const manager = createManager();
    manager.fromXML(`<page${attribute}><section${attribute}><column${attribute}><widget name="content"${attribute}/></column></section></page>`);

    const structure = JSON.parse(JSON.stringify(manager.getStructure()));
    const row = structure.rows[0];
    const column = row.columns[0];
    const widget = column.widgets[0];
    for (const item of [structure, row, column, widget]) {
      expect(item.role).toEqual(expectedRoles);
      expect(item.rolePresent).toBe(attribute !== '');
    }

    const xml = manager.toXML();
    const previewXml = manager.toXML(true);
    for (const output of [xml, previewXml]) {
      const document = new DOMParser().parseFromString(output, 'text/xml');
      for (const tag of ['page', 'section', 'column', 'widget']) {
        const element = document.getElementsByTagName(tag)[0];
        expect(element.hasAttribute('role')).toBe(attribute !== '');
        if (attribute) expect(element.getAttribute('role')).toBe(expectedRoles.join(','));
      }
    }
  });

  test('escapes role values in generated XML', () => {
    const manager = createManager();
    manager.fromXML('<page role="A&amp;B"><section><column><widget name="content"/></column></section></page>');
    expect(manager.toXML()).toContain('<page role="A&amp;B">');
  });
});