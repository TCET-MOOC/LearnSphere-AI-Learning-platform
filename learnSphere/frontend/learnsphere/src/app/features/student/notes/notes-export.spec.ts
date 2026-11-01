import { buildNotesExportHtml, escapeHtml } from './notes-export';
import { Note } from '@core/models/course.model';

/** Post-W6 audit: the notes PDF export wrote titles, course names and tags into a same-origin window unescaped. */
describe('notes export (audit fix: script injection)', () => {
  const evil = '<img src=x onerror="alert(1)">';
  const note = {
    id: 1, courseId: 1, courseName: `Course ${evil}`, lectureId: 1, lectureLabel: `Lec ${evil}`,
    timestampSeconds: 154, title: `Title ${evil}`, content: `Body ${evil} **bold**`, tags: [`tag${evil}`],
    createdAt: '2026-11-01T10:00:00', updatedAt: '2026-11-01T10:00:00'
  } as unknown as Note;

  it('escapes HTML special characters', () => {
    expect(escapeHtml(`<a href="x" title='y'>&</a>`)).toBe('&lt;a href=&quot;x&quot; title=&#39;y&#39;&gt;&amp;&lt;/a&gt;');
    expect(escapeHtml(null)).toBe('');
  });

  it('never writes a teacher- or student-supplied value as markup', () => {
    const html = buildNotesExportHtml([note], `Study Notes - ${evil}`);
    expect(html).not.toContain('<img');
    expect(html).not.toContain('onerror="');
    expect(html).toContain('Title &lt;img src=x onerror=&quot;alert(1)&quot;&gt;');
    expect(html).toContain('Course &lt;img');
    expect(html).toContain('Lec &lt;img');
    expect(html).toContain('#tag&lt;img');
    expect(html).toContain('<title>Study Notes - &lt;img');
  });

  it('keeps the light markdown formatting of the note body', () => {
    expect(buildNotesExportHtml([note], 'All Study Notes')).toContain('<strong>bold</strong>');
  });
});
