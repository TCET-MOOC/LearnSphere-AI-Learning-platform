import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface TableColumn {
  key: string;
  header: string;
  sortable?: boolean;
  type?: 'text' | 'number' | 'percent' | 'progress' | 'avatar' | 'action' | 'status-pill';
}

/**
 * DataTableComponent is a reusable dashboard grid table that wraps sorting, 
 * pagination, and customizable cell rendering (avatars, progress bars, percentages).
 */
@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="data-table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th *ngFor="let col of columns" 
                [class.sortable]="col.sortable"
                (click)="col.sortable ? onSort(col.key) : null">
              {{ col.header }}
              <span *ngIf="col.sortable && sortKey === col.key" class="sort-direction">
                {{ sortDirection === 'asc' ? ' ▲' : ' ▼' }}
              </span>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let row of paginatedData">
            <td *ngFor="let col of columns">
              <ng-container [ngSwitch]="col.type">
                
                <ng-container *ngSwitchCase="'avatar'">
                  <div class="avatar-cell">
                    <span class="avatar-initials">{{ getInitials(row[col.key] || row['name']) }}</span>
                  </div>
                </ng-container>

                <ng-container *ngSwitchCase="'progress'">
                  <div class="progress-cell">
                    <span class="progress-text">{{ row[col.key] }} / {{ row.totalLectures || 20 }}</span>
                    <div class="pbar">
                      <div class="pfill" [style.width.%]="(row[col.key] / (row.totalLectures || 20)) * 100"></div>
                    </div>
                  </div>
                </ng-container>

                <ng-container *ngSwitchCase="'percent'">
                  <span>{{ row[col.key] }}%</span>
                </ng-container>

                <ng-container *ngSwitchCase="'action'">
                  <button class="btn btn--primary btn--sm" (click)="onRowAction(row, col.key)">
                    {{ col.header }}
                  </button>
                </ng-container>

                <ng-container *ngSwitchDefault>
                  {{ row[col.key] }}
                </ng-container>

              </ng-container>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination controls -->
      <div class="pagination" *ngIf="totalPages > 1">
        <button class="btn btn--ghost btn--sm" [disabled]="currentPage === 1" (click)="setPage(currentPage - 1)">
          Previous
        </button>
        <span class="pagination-info">Page {{ currentPage }} of {{ totalPages }}</span>
        <button class="btn btn--ghost btn--sm" [disabled]="currentPage === totalPages" (click)="setPage(currentPage + 1)">
          Next
        </button>
      </div>
    </div>
  `,
  styles: [`
    @import '../../../../styles/variables';

    .data-table-container {
      width: 100%;
      overflow-x: auto;
    }

    .data-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 12px;
      text-align: left;
      
      thead {
        background-color: var(--color-background-secondary);
        border-bottom: 0.5px solid var(--color-border-tertiary);
        
        th {
          padding: 10px;
          font-weight: 500;
          color: var(--color-text-secondary);
          user-select: none;
          
          &.sortable {
            cursor: pointer;
            &:hover {
              color: var(--color-text-primary);
            }
          }
        }
      }
      
      tbody {
        tr {
          border-bottom: 0.5px solid var(--color-border-tertiary);
          &:last-child {
            border-bottom: none;
          }
          
          td {
            padding: 10px;
            color: var(--color-text-primary);
            vertical-align: middle;
          }
        }
      }
    }

    .avatar-cell {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      background: #EEEDFE;
      color: #3C3489;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 10px;
      font-weight: 500;
    }

    .progress-cell {
      display: flex;
      flex-direction: column;
      gap: 4px;
      width: 100px;
      
      .progress-text {
        font-size: 10px;
        color: var(--color-text-secondary);
      }
      
      .pbar {
        height: 4px;
        background-color: var(--color-border-tertiary);
        border-radius: 2px;
        width: 100%;
        overflow: hidden;
      }
      
      .pfill {
        height: 100%;
        background-color: $brand-purple;
        border-radius: 2px;
      }
    }

    .pagination {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px;
      border-top: 0.5px solid var(--color-border-tertiary);
      
      &-info {
        font-size: 11px;
        color: var(--color-text-secondary);
      }
    }
  `]
})
export class DataTableComponent implements OnInit, OnChanges {
  @Input() columns: TableColumn[] = [];
  @Input() data: any[] = [];
  @Input() pageSize: number = 20;

  @Output() rowAction = new EventEmitter<{ row: any; action: string }>();

  currentPage = 1;
  sortKey: string = '';
  sortDirection: 'asc' | 'desc' = 'asc';

  sortedData: any[] = [];
  paginatedData: any[] = [];

  ngOnInit(): void {
    this.processData();
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.processData();
  }

  /**
   * Applies sort mappings and filters data segments by current page offset.
   */
  processData(): void {
    let result = [...this.data];

    // Apply Sorting
    if (this.sortKey) {
      result.sort((a, b) => {
        const valA = a[this.sortKey];
        const valB = b[this.sortKey];
        if (valA < valB) return this.sortDirection === 'asc' ? -1 : 1;
        if (valA > valB) return this.sortDirection === 'asc' ? 1 : -1;
        return 0;
      });
    }

    this.sortedData = result;
    this.setPage(this.currentPage);
  }

  get totalPages(): number {
    return Math.ceil(this.sortedData.length / this.pageSize);
  }

  /**
   * Shifts display indices to slice data elements by selected page.
   */
  setPage(page: number): void {
    if (page < 1 || (this.totalPages > 0 && page > this.totalPages)) return;
    this.currentPage = page;
    const startIdx = (page - 1) * this.pageSize;
    this.paginatedData = this.sortedData.slice(startIdx, startIdx + this.pageSize);
  }

  /**
   * Toggles sorting properties.
   */
  onSort(key: string): void {
    if (this.sortKey === key) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortKey = key;
      this.sortDirection = 'asc';
    }
    this.processData();
  }

  /**
   * Passes the active row click back to the calling view container.
   */
  onRowAction(row: any, colKey: string): void {
    this.rowAction.emit({ row, action: colKey });
  }

  /**
   * Utility helper extracting capital initials from full name text.
   */
  getInitials(name: string): string {
    if (!name) return '';
    return name.split(' ').map(w => w[0]).join('').substring(0, 2).toUpperCase();
  }
}
