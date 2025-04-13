import { Component, inject, output } from '@angular/core';
import { ParamMap, RouterLink } from '@angular/router';
import { SharedDataService } from '../shareddata.service';
import { ActivatedRoute } from '@angular/router';
import { Observable, timer, Subject } from 'rxjs';
import { switchMap, takeUntil } from 'rxjs/operators';
import {HttpClient} from '@angular/common/http';

const channel = new BroadcastChannel('succes-paypal-channel');

@Component({
  selector: 'app-success',
  imports: [],
  templateUrl: './success.component.html',
  styleUrl: './success.component.css',
  standalone: true,
})
export class SuccessComponent {

  
  private readonly route = inject(ActivatedRoute);
  paymentId: string | null = null;
  PayerID: string | null = null;

  ngOnInit() {
  
    //Necesario para leer los params en ngOnInit... :P
    setTimeout(()=>{
      channel.postMessage({message: 'successPaypal', 
        paymentId: this.route.snapshot.queryParamMap.get('paymentId'),
        PayerID: this.route.snapshot.queryParamMap.get('PayerID')
      });
      this.onClose();

    }, 100);
    
  }

  onClose() {

    window.close();    

  }

}
