import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subject } from 'rxjs';

const channel = new BroadcastChannel('succes-paypal-channel');

@Component({
  selector: 'app-cancel',
  imports: [RouterLink],
  templateUrl: './cancel.component.html',
  styleUrl: './cancel.component.css'
})
export class CancelComponent {

  private readonly route = inject(ActivatedRoute);
  paymentId: string | null = null;
  PayerID: string | null = null;


  ngOnInit() {
  
    //Necesario para leer los params en ngOnInit... :P
    setTimeout(()=>{
      channel.postMessage({message: 'cancelPaypal', 
        paymentId: null,
        PayerID: null
      });
      this.onClose();

    }, 100);
    
  }

  onClose() {

    window.close();    

  }


}
