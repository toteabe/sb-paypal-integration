import { Routes } from '@angular/router';
import { PaypalComponent } from './paypal/paypal.component';
import { SuccessComponent } from './paypal/success/success.component';
import { CancelComponent } from './paypal/cancel/cancel.component';

export const routes: Routes = [ { path: '', redirectTo: 'paypal', pathMatch: 'full' },
    { path: 'paypal', component: PaypalComponent},
    { path: 'payment/success', component: SuccessComponent},
    { path: 'payment/cancel', component: CancelComponent},

];

