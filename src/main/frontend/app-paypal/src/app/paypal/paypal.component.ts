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

@Component({
  selector: 'app-paypal',
  imports: [ReactiveFormsModule],
  templateUrl: './paypal.component.html',
  styleUrl: './paypal.component.css',
  standalone: true
})
export class PaypalComponent {

  apiUrl: string =  'http://localhost:8080/payment/create'; //
  
  http = inject(HttpClient);
  router = inject(Router);
  sharedDataService = inject(SharedDataService);
  cd = inject(ChangeDetectorRef);

  showsProcessPaypal: boolean = false;
  urlApproval: string = '';

  fb = inject(FormBuilder);
  paypalForm: FormGroup = this.fb.group({
    method: ['Paypal', Validators.required],
    amount: ['', Validators.required],
    currency: ['EUR', Validators.required],
    description: ['', Validators.required],
  })

  ngOnInit() {
    
    const channel = new BroadcastChannel('succes-paypal-channel');
    channel.onmessage = (event) => {
      if (event.data === 'closeSuccessPaypal') {
        this.router.navigate(['']);
        this.showsProcessPaypal = false; 
        this.cd.detectChanges();
      }
    };
  }
  
  onSubmit() {

    this.http.post<PaypalResponse>(this.apiUrl, this.paypalForm.value ).subscribe((data) => {
      if (data.approvalUrl) {
        this.showsProcessPaypal = true;
        this.urlApproval = data.approvalUrl;
        window.open(data.approvalUrl ,"popup" ,"width=390,height=844");
      }
    });


  }

}
