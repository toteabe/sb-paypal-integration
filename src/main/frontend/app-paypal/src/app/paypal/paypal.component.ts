import { ChangeDetectorRef, Component, inject } from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { PaypalResponse } from './paypalresponse.interface';
import { SafePipe } from './safe.pipe';
import { Router } from '@angular/router';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { SharedDataService } from './shareddata.service';
import { Observable, timer, Subject } from 'rxjs';
import { switchMap, takeUntil } from 'rxjs/operators';

const channel = new BroadcastChannel('succes-paypal-channel');

@Component({
  selector: 'app-paypal',
  imports: [ReactiveFormsModule],
  templateUrl: './paypal.component.html',
  styleUrl: './paypal.component.css',
  standalone: true
})
export class PaypalComponent {

  apiUrl: string =  'http://localhost:8080'; //
  
  http = inject(HttpClient);
  router = inject(Router);
  sharedDataService = inject(SharedDataService);
  cd = inject(ChangeDetectorRef);

  cancelado: boolean = false;
  procesando: boolean = false;
  showFormPaypal: boolean = true;
  urlApproval: string = '';
  orderId: string = '';

  fb = inject(FormBuilder);
  paypalForm: FormGroup = this.fb.group({
    method: ['Paypal', Validators.required],
    amount: ['', Validators.required],
    currency: ['EUR', Validators.required],
    description: ['', Validators.required],
  })

  closeTimer$ = new Subject<any>();

  ngOnInit() {
    
    channel.onmessage = (event) => {
      if (event.data.message === 'successPaypal') {
        this.polling(this.orderId);
      } else if (event.data.message === 'cancelPaypal') {
        this.procesando = false;
        this.showFormPaypal = false;
        this.cancelado = true;
        this.cd.detectChanges();
      }
    };


  }
  
  onSubmit() {

    this.http.post<PaypalResponse>(this.apiUrl+'/payment/create', this.paypalForm.value ).subscribe((data) => {
      if (data.href) {        
        this.procesando = true;
        this.showFormPaypal = false;
        this.orderId = data.orderId;
        this.urlApproval = data.href;
        window.open(data.href ,"popup" ,"width=390,height=844");
      }
    });

  }

  sendPolling(orderId: string): Observable<any> {
    return this.http.get<any>(this.apiUrl+'/payment/success',{params: {orderId} });
  }

  onClose() {

    this.showFormPaypal=true;
    this.procesando=false;
    this.cd.detectChanges();
    
  }

  polling(orderId: string) {

    this.sendPolling(orderId).subscribe((data: any) =>{
      if (data["state"]=="APPROVED") {
        // <-- para las solicitudes de polling
  
        //this.router.navigate(['']);
        this.showFormPaypal = false;        
        this.procesando = false;                
        this.cd.detectChanges();
      
      } else {
        setTimeout(() =>{this.polling(orderId)}, 200);
      }

    });

  }
}