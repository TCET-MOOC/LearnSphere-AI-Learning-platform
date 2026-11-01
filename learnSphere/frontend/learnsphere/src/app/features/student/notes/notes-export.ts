import { Note } from '@core/models/course.model';

/**
 * Builds the printable notes export (post-W6 audit fix). Every value that came from a user
 * (note title, course name, lecture label, tags, the document title) is HTML-escaped: the
 * export is written into a same-origin window, so raw values would run as script.
 */
export function escapeHtml(value: unknown): string {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/** Escapes, then applies the light markdown used in notes (bold, italic, code, bullets). */
function formatMarkdown(content: string): string {
  if (!content) return '';
  let escaped = escapeHtml(content);
  escaped = escaped.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
  escaped = escaped.replace(/\*(.*?)\*/g, '<em>$1</em>');
  escaped = escaped.replace(/`(.*?)`/g, '<code>$1</code>');
  escaped = escaped.replace(/^-\s+(.*)$/gm, '&bull; $1');
  return escaped;
}

function formatDuration(seconds: number): string {
  const total = Math.max(0, Math.floor(seconds || 0));
  const mins = Math.floor(total / 60);
  const secs = total % 60;
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
}

export function buildNotesExportHtml(notesList: Note[], docTitle: string): string {
  const notesHTML = notesList.map(note => {
    const formattedContent = formatMarkdown(note.content);
    const formattedDate = new Date(note.createdAt).toLocaleDateString(undefined, {
      year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit'
    });
    const durationFormatted = formatDuration(note.timestampSeconds);
    const tagsHTML = note.tags.map(t => `<span class="tag">#${escapeHtml(t)}</span>`).join(' ');

    return `
      <div class="note-item">
        <div class="note-header">
          <h2 class="note-title">${escapeHtml(note.title)}</h2>
          <div class="note-meta">
            <span class="meta-item"><strong>Course:</strong> ${escapeHtml(note.courseName)}</span>
            <span class="meta-item"><strong>Lecture:</strong> ${escapeHtml(note.lectureLabel)} (⏱ ${durationFormatted})</span>
            <span class="meta-item"><strong>Date:</strong> ${escapeHtml(formattedDate)}</span>
          </div>
        </div>
        <div class="note-body">${formattedContent}</div>
        ${note.tags.length > 0 ? `<div class="note-tags">${tagsHTML}</div>` : ''}
      </div>
    `;
  }).join('<hr class="divider" />');


  return `
      <!DOCTYPE html>
      <html>
        <head>
          <title>${escapeHtml(docTitle)}</title>
          <style>
            @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');
            body {
              font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
              color: #1e293b;
              line-height: 1.5;
              padding: 40px;
              max-width: 800px;
              margin: 0 auto;
              background-color: #ffffff;
            }
            .header {
              border-bottom: 2px solid #534ab7;
              padding-bottom: 20px;
              margin-bottom: 35px;
              display: flex;
              justify-content: space-between;
              align-items: flex-end;
            }
            .header-title {
              font-size: 24px;
              font-weight: 700;
              color: #3c3489;
              margin: 0;
            }
            .header-subtitle {
              font-size: 14px;
              color: #64748b;
              margin: 4px 0 0 0;
            }
            .header-date {
              font-size: 12px;
              color: #64748b;
              text-align: right;
            }
            .note-item {
              margin-bottom: 30px;
              page-break-inside: avoid;
            }
            .note-title {
              font-size: 18px;
              font-weight: 600;
              color: #1e293b;
              margin: 0 0 8px 0;
            }
            .note-meta {
              font-size: 12px;
              color: #64748b;
              margin-bottom: 12px;
              display: flex;
              flex-wrap: wrap;
              gap: 15px;
            }
            .meta-item {
              display: inline-block;
            }
            .note-body {
              font-size: 14px;
              color: #334155;
              white-space: pre-wrap;
              background-color: #f8fafc;
              border-left: 3px solid #534ab7;
              padding: 12px 16px;
              border-radius: 6px;
              margin-bottom: 12px;
              border: 1px solid #e2e8f0;
              border-left-width: 3px;
            }
            .note-body strong {
              font-weight: 600;
              color: #0f172a;
            }
            .note-body code {
              background-color: rgba(83, 74, 183, 0.08);
              color: #3c3489;
              padding: 2px 5px;
              border-radius: 4px;
              font-family: monospace;
              font-size: 13px;
            }
            .note-tags {
              display: flex;
              flex-wrap: wrap;
              gap: 6px;
              margin-top: 8px;
            }
            .tag {
              display: inline-block;
              font-size: 11px;
              font-weight: 500;
              background-color: #eeedfe;
              color: #3c3489;
              padding: 2px 8px;
              border-radius: 9999px;
              border: 0.5px solid rgba(83, 74, 183, 0.15);
            }
            .divider {
              border: 0;
              border-top: 1px solid #e2e8f0;
              margin: 30px 0;
            }
            @media print {
              body {
                padding: 0;
              }
              .divider {
                border-top: 1px dashed #cbd5e1;
              }
            }
          </style>
        </head>
        <body>
          <div class="header">
            <div>
              <h1 class="header-title">LearnSphere Study Notes</h1>
              <p class="header-subtitle">${escapeHtml(docTitle)}</p>
            </div>
            <div class="header-date">
              <strong>Export Date:</strong> ${escapeHtml(new Date().toLocaleDateString())}<br/>
              <strong>Notes Count:</strong> ${notesList.length}
            </div>
          </div>
          <div class="notes-list">
            ${notesHTML}
          </div>
          <script>
            window.onload = function() {
              window.print();
            }
          </script>
        </body>
      </html>
    `;
}
